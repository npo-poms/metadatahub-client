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




        // Collect multi-valued fields across all rows

        // todo, I think the number of considered rows is a bit over the top
        // we should use unions or group by or so.
        List<ScheduleEvent> scheduleEvents = new ArrayList<>();
        Set<String> genreIds = new LinkedHashSet<>();
        AgeRating ageRating = null;
        Set<ContentRating> contentRatings = new LinkedHashSet<>();
        Set<Broadcaster> broadcasters = new LinkedHashSet<>();

        while(resultSet.hasNext()) {
            QuerySolution row = resultSet.next();
            toScheduleEvent(fields, row).ifPresent(scheduleEvents::add);

            if (fields.contains("country")) {
                setString("country", fields, row, builder::countries);

            }
            // Genre
            if (fields.contains("genreId")) {
                Literal genreLit = row.getLiteral("genreId");
                if (genreLit != null) {
                    genreIds.add(genreLit.getString());
                }
            }

            // Rating: age vs content, determined by ratingType URI
            if (fields.contains("ratingType") && fields.contains("ratingValue")) {
                Literal typeLit  = row.getLiteral("ratingType");
                Literal valueLit = row.getLiteral("ratingValue");
                if (typeLit != null && valueLit != null) {
                    String ratingType = typeLit.getString();
                    if (ratingType.contains(AGE_RATING_TYPE_SUFFIX)) {
                        // e.g. ratingValue = "6", "12", "ALL"
                        ageRating = parseAgeRating(valueLit.getString());
                    } else {
                        // content warning: GEWELD, SEKS, ANGST, etc.
                        parseContentRating(valueLit.getString()).ifPresent(contentRatings::add);
                    }
                }
            }
            // Broadcaster
            if (fields.contains("broadcaster")) {
                Literal broadcasterLit = row.getLiteral("broadcaster");
                if (broadcasterLit != null) {
                    ServiceLocator.getBroadcasterService().findForIds(broadcasterLit.getString())
                        .ifPresentOrElse(
                            broadcasters::add
                            ,
                            () -> log.warning("Broadcaster " + broadcasterLit + " snot found")
                        );

                }
            }
        }

        builder.scheduleEvents(scheduleEvents.toArray(new ScheduleEvent[0]));
        builder.genres(genreIds.stream().map(this::matchTermId).filter(Optional::isPresent).map(Optional::get).toList());
        builder.ageRating(ageRating);
        builder.contentRatings(contentRatings.toArray(new ContentRating[0]));
        builder.broadcasters(broadcasters.toArray(new Broadcaster[0]));
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

    private Optional<Genre> loadGenre(String key) {
        String[] split = key.split("-", 2);
        String primary = split[0].trim();
        String secondary;
        if  (split.length > 1) {
            secondary = split[1].trim();
        } else {
            secondary = null;
        }
        Optional<Term> primaryTerm = classificationService.values().stream()
            .filter(t -> t.depth() == 4)
            .filter(k -> k.getName(nl_vpp).equalsIgnoreCase(primary)).findFirst();

        if (primaryTerm.isEmpty()) {
            log.warning("Could not find primary term for " + primary);
            return Optional.empty();
        }

        final Optional<Term> secondaryTerm = classificationService.values().stream()
            .filter(t -> primaryTerm.get().equals(t.getParent()))
            .filter(k -> k.getName(nl_vpp).equalsIgnoreCase(secondary))
            .findFirst();


        if (secondaryTerm.isPresent()) {
            return Optional.of(Genre.of(secondaryTerm.get()));
        } else {
            if (StringUtils.isNotEmpty(secondary)) {
                if (! "Overig".equalsIgnoreCase(secondary)) {
                    throw new NoSuchElementException("No such term " + secondary + " for " + primaryTerm);
                }
            }
        }
        return Optional.of(Genre.of(primaryTerm.get()));
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
}
