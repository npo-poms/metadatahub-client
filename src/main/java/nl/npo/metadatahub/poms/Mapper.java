package nl.npo.metadatahub.poms;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import lombok.extern.java.Log;
import nl.vpro.domain.classification.*;
import nl.vpro.domain.media.*;
import nl.vpro.domain.media.support.OwnerType;
import nl.vpro.domain.user.Broadcaster;
import nl.vpro.domain.user.ServiceLocator;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.datatypes.xsd.XSDDateTime;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;

/**
 * Contains the logic to map jena {@link ResultSet 's } to objects from the poms domain.
 */
@Log
public class Mapper {

    private static final Locale nl_vpp = Locale.of("nl", "", "vpp");


    // ratingType URI suffixes that indicate an age rating (Kijkwijzer)
    private static final String AGE_RATING_TYPE_SUFFIX = "AgeRating";



    private final ClassificationService classificationService;

    protected Mapper() {
        this.classificationService = MetadataHubService.classificationService;
    }

    /** Maps scalar fields from the first row; collects ScheduleEvents, genres and ratings from all rows. */
    public boolean  toProgram(ResultSet resultSet, MediaBuilder.ProgramBuilder builder) {
        if (!resultSet.hasNext()) {
            return false;
        }
        List<String> fields = resultSet.getResultVars();
        QuerySolution first = resultSet.next();

        setString("title", fields, first, t -> builder.mainTitle(t, OwnerType.AUTHORITY));
        setString("description", fields, first, d -> builder.mainDescription(d, OwnerType.AUTHORITY));
        setString("prid", fields , first, builder::mid);
        setInstant("dateCreated", fields, first, builder::creationInstant);
        setInstant("dateModified", fields, first, builder::lastModified);


        String[] broadcasters = split(first.getLiteral("broadcasters"));
        builder.broadcasters(broadcasters);

        String[] countries = split(first.getLiteral("countries"));
        builder.countries(countries);

        String[] genres = split(first.getLiteral("genres"));
        builder.genres(Arrays.stream(genres).map(this::matchTermId).filter(Optional::isPresent).map(Optional::get).toList());

        String[] contentRatings = split(first.getLiteral("contentRatings"));
        builder.contentRatings(
            Arrays.stream(contentRatings)
                .map(this::parseContentRating)
                .filter(Optional::isPresent)
                .map(Optional::get).toArray(i -> new ContentRating[i])
        );



        String[]  scheduleEventsSplit = split(first.getLiteral("scheduleEvents"));
        for (String scheduleEvent : scheduleEventsSplit) {
            builder.scheduleEvent(parseScheduleEvent(scheduleEvent));

        }

        return true;
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


    public Optional<ScheduleEvent> toScheduleEvent(List<String> fields, QuerySolution row) {
        if (!fields.contains("channelName") || !fields.contains("start")) {
            return Optional.empty();
        }
        Literal channelLit = row.getLiteral("channelName");
        Literal startLit   = row.getLiteral("start");
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
        String startStr    = parts[1];
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

    protected void setString(String field, List<String> fields, QuerySolution item, Consumer<String> consumer) {
        set(field, fields, item, consumer, Literal::getString);
    }

    protected void setInstant(String field, List<String> fields, QuerySolution item, Consumer<Instant> consumer) {
        set(field, fields, item, consumer, lit -> ((XSDDateTime) lit.getValue()).asCalendar().toInstant());
    }


    protected <T> void set(String field, List<String> fields, QuerySolution item, Consumer<T> consumer, Function<Literal, T> converter) {
        if (fields.contains(field)) {
            var lit = item.getLiteral(field);
            if (lit == null) {
                consumer.accept(null);
            } else {
                consumer.accept(converter.apply(lit));
            }
        }
    }

    String[] split(Literal lit){
        if (lit == null) {
            return new String[0];
        }
        String s= lit.getString();

        if (s.isEmpty()) {
            return new String[0];
        } else {
            return s.split("\\|");

        }
    }
}
