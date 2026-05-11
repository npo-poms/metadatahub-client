package nl.npo.metadatahub.poms;

import java.lang.reflect.Array;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import lombok.extern.java.Log;
import nl.vpro.domain.classification.*;
import nl.vpro.domain.media.*;
import nl.vpro.domain.media.support.*;
import nl.vpro.domain.user.Broadcaster;
import nl.vpro.domain.user.ServiceLocator;
import org.apache.jena.datatypes.xsd.XSDDateTime;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFNode;
import org.meeuw.functional.Functions;

/**
 * Contains the logic to map jena {@link ResultSet 's } to objects from the poms domain.
 */
@Log
public class Mapper {

    private final ClassificationService classificationService;

    protected Mapper() {
        this.classificationService = MetadataHubService.classificationService;
    }


    public void toProgram(QuerySolution row, MediaBuilder.ProgramBuilder builder) {
        toMediaObject(row, builder);
        String[] scheduleEventsSplit = split(row.getLiteral("scheduleEvents"));
        for (String scheduleEvent : scheduleEventsSplit) {
            builder.scheduleEvent(parseScheduleEvent(scheduleEvent));
        }

        setMultipleValue("episodeOfSeasons", row,  MemberRef.class, r -> new MemberRef(r, 1),
            builder::episodeOf);
    }

    public <B extends MediaBuilder<B, M>, M extends MediaObject> void toMediaObject(QuerySolution row, MediaBuilder<B, M> builder) {

        setString("title", row, t -> builder.mainTitle(t, OwnerType.AUTHORITY));
        // alternative titles?
        setString("description", row, d -> builder.mainDescription(d, OwnerType.AUTHORITY));
        setMultipleValue("alternativeDescriptions", row, Description.class, this::parseDescription, builder::descriptions);

        setString("prid", row, builder::mid, false);
        setInstant("dateCreated", row, builder::creationInstant);
        setInstant("dateModified", row, builder::lastModified);

        if (row.get("ebuFormat") != null) {
            AVType avType = switch (row.get("ebuFormat").asResource().getURI()) {
                case "http://www.ebu.ch/metadata/ontologies/ebucoreplus#VideoFormat" -> AVType.VIDEO;
                case "http://www.ebu.ch/metadata/ontologies/ebucoreplus#AudioFormat" -> AVType.AUDIO;
                default -> throw new IllegalArgumentException(row.get("ebuformat").toString());
            };
            builder.avType(avType);
        }

        setStringFromResource("entity", row, c -> builder.crids(c.replaceAll("^http://", "crid://")));
        setMultipleString("crids", row,  builder::crids);
        setMultipleValue("broadcasters", row, Broadcaster.class, c -> ServiceLocator.getBroadcasterService().findForIds(c).orElseThrow(),  builder::broadcasters);
        setMultipleString("countries", row, builder::countries);
        setMultipleValue("genres", row,  Genre.class, s -> Objects.requireNonNull(matchTermId(s).orElse(null)), builder::genres);
        set("ageRating", row, s -> parseAgeRating(s.getString()), builder::ageRating, false);
        setMultipleValue("contentRatings", row,  ContentRating.class, s -> Objects.requireNonNull(parseContentRating(s).orElse(null)), builder::contentRatings);
        setMultipleValue("persons", row,  Person.class, s -> Objects.requireNonNull(parsePerson(s).orElse(null)), builder::persons);
    }


    private Optional<Genre> matchTermId(String termId) {
        try {
            Term t = classificationService.getTerm(termId);
            return Optional.of(Genre.of(t));
        } catch (TermNotFoundException termNotFoundException) {
            log.severe("Unknown term id  '%s', ignoring".formatted(termId));
            return Optional.empty();

        }
    }


    private AgeRating parseAgeRating(String value) {
        if ("".equals(value)) {
            return null;
        }
        if ("AL".equals(value.trim())) {
            return AgeRating.ALL;
        }
        try {
            return AgeRating.valueOf(Integer.parseInt(value.trim()));
        } catch (NumberFormatException e) {
            try {
                return AgeRating.xmlValueOf(value.trim());
            } catch (Exception ex) {
                log.severe("Unknown age rating value '%s', ignoring".formatted(value));
                return null;
            }
        }
    }

    private Optional<ContentRating> parseContentRating(String value) {
        if ("AL".equals(value.trim())) {
            return Optional.empty();
        }
        // also this seems to be matched on display avalue.....
        return Arrays.stream(ContentRating.values()).filter(c -> c.getDisplayName().equalsIgnoreCase(value)).findFirst();
    }

    private Optional<Person> parsePerson(String value) {
        String[] fields = value.split("\t");
        Person person = Person.builder()
            .name(fields[0])
            .role(RoleType.valueOf(fields[1].toUpperCase()))
            .gtaaUri(fields.length > 2 ? fields[2] : null)
            .build();
        return Optional.of(person);
    }

    private Description parseDescription(String value) {
        String[] fields = value.split(":",2);
        return new Description(fields[1], OwnerType.AUTHORITY, parseTextualType(fields[0]));
    }

    private TextualType parseTextualType(String value) {
        return switch (value.toLowerCase()) {
            case "medium description" -> TextualType.MEDIUM;
            case "short description" -> TextualType.SHORT;
            case "kicker description" -> TextualType.KICKER;
            //case "styled description" -> TextualType.MARKDOWN;
            default ->  throw new RuntimeException();
        };
    }


    public Optional<ScheduleEvent> toScheduleEvent(List<String> fields, QuerySolution row) {
        if (!fields.contains("channelName") || !fields.contains("start")) {
            return Optional.empty();
        }
        Literal channelLit = row.getLiteral("channelName");
        Literal startLit = row.getLiteral("start");
        if (channelLit == null || startLit == null) {
            return Optional.empty();
        }
        Channel channel = getChannelByDisplayName(channelLit.getString()).orElse(null);
        if (channel == null) {
            return Optional.empty();
        }
        Instant start = ((XSDDateTime) startLit.getValue()).asCalendar().toInstant();

        Duration duration = Duration.ZERO;
        if (fields.contains("end")) {
            Literal endLit = row.getLiteral("end");
            if (endLit != null) {
                Instant end = ((XSDDateTime) endLit.getValue()).asCalendar().toInstant();
                duration = Duration.between(start, end);
            }
        }
        return Optional.of(new ScheduleEvent(channel, start, duration));
    }

    /**
     * Parse the aggregated ?scheduleEvents field (triplets: channelName|start|duration, separated by "|").
     * Returns one ScheduleEvent per triplet.
     */
    public ScheduleEvent parseScheduleEvent(String scheduleEvents) {

        String[] parts = scheduleEvents.split(",", 3);
        List<ScheduleEvent> result = new ArrayList<>();
        // triplets: channelName, start, duration
        String channelName = parts[0];
        String startStr = parts[1];
        String durationStr = parts[2];
        Channel channel = getChannelByDisplayName(channelName).orElse(null);
        Instant start = Instant.parse(startStr);
        Duration duration = null;
        if (!durationStr.isEmpty()) {
            duration = Duration.ofSeconds(Math.round(1000 * Float.parseFloat(durationStr)));
        }
        return new ScheduleEvent(channel, start, duration);

    }

    public static Optional<Channel> getChannelByDisplayName(String name) {
        if ("INTERNETVOD".equals(name)) {
            return Optional.of(Channel.NVOD);
        }
        if ("PLUSVOD".equals(name)) {
            return Optional.of(Channel.NVOD);
        }
        if ("BVN".equals(name)) {
            return Optional.of(Channel.BVNT);
        }
        if ("NPO 3 & Zapp".equals(name)) {
            return Optional.of(Channel.NED3);
        }
        if ("NPO Radio 1".equals(name)) {
            return Optional.of(Channel.RAD1);
        }
        for (Channel channel : Channel.values()) {
            if (channel.getDisplayName().equalsIgnoreCase(name)) {
                return Optional.of(channel);
            }
        }
        log.severe("No channel found with name " + name);
        return Optional.empty();
    }

    protected void setString(String field, QuerySolution item, Consumer<String> consumer, boolean setToNull) {
        set(field, item, Literal::getString, consumer,  setToNull);
    }
    protected void setStringFromResource(String field, QuerySolution item, Consumer<String> consumer) {
        RDFNode value = item.get(field);
        consumer.accept(String.valueOf(value));
    }


    protected void setString(String field, QuerySolution item, Consumer<String> consumer) {
        setString(field, item, consumer, true);
    }

    protected void setInstant(String field, QuerySolution item, Consumer<Instant> consumer) {
        set(field, item, lit -> ((XSDDateTime) lit.getValue()).asCalendar().toInstant(), consumer, true);
    }

    protected <S> void setMultipleString(String field, QuerySolution item,  Consumer<String[]> consumer) {
        setMultipleValue(field, item, String.class, Functions.identity(), consumer);
    }
    @SuppressWarnings("unchecked")
    protected <S> void setMultipleValue(String field, QuerySolution item, Class<S> clazz, Function<String, S> converter, Consumer<S[]> consumer) {
        String[] values = split(item.getLiteral(field));
        S[] array =  Arrays.stream(values)
            .map(converter)
            .toArray(size -> (S[]) Array.newInstance(clazz, size));
        consumer.accept(array);
    }

    /**
     *
     *
     * @param setToNull mainly for fields that are not always queried, like mid.
     * @param <T>
     */
    protected <T> void set(String field, QuerySolution item,  Function<Literal, T> converter, Consumer<T> consumer, boolean setToNull) {
        var lit = item.getLiteral(field);
        if (lit == null) {
            if (setToNull) {
                consumer.accept(null);
            }
        } else {
            consumer.accept(converter.apply(lit));
        }
    }

    String[] split(Literal lit) {
        if (lit == null) {
            return new String[0];
        }
        String s = lit.getString();

        if (s.isEmpty()) {
            return new String[0];
        } else {
            return s.split("\\|");

        }
    }
    @SuppressWarnings("unchecked")
    public static <T> T[] createArray(int size, T defaultValue)
    {
        // Creating a generic array using reflection
        T[] array = (T[]) Array.newInstance(defaultValue.getClass(), size);

        Arrays.fill(array, defaultValue);
        return array;
    }
}

