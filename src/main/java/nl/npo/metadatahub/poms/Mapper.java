package nl.npo.metadatahub.poms;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import lombok.extern.java.Log;
import nl.vpro.domain.media.AgeRating;
import nl.vpro.domain.media.Channel;
import nl.vpro.domain.media.ContentRating;
import nl.vpro.domain.media.Genre;
import nl.vpro.domain.media.MediaBuilder;
import nl.vpro.domain.media.ScheduleEvent;
import org.apache.jena.datatypes.xsd.XSDDateTime;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.Literal;

@Log
public class Mapper {


    // ratingType URI suffixes that indicate an age rating (Kijkwijzer)
    private static final String AGE_RATING_TYPE_SUFFIX = "AgeRating";

    private final Set<String> fields;

    public Mapper(Collection<String> fields) {
        this.fields = new HashSet<>(fields);
    }

    /** Maps scalar fields from the first row; collects ScheduleEvents, genres and ratings from all rows. */
    public void  toProgram(List<QuerySolution> rows, MediaBuilder.ProgramBuilder builder) {
        QuerySolution first = rows.getFirst();
        setString("title", first, builder::mainTitle);
        setString("description", first, builder::mainDescription);
        setString("prid", first, builder::mid);
        setInstant("dateCreated", first, builder::creationInstant);
        setInstant("dateModified", first, builder::lastModified);

        // Collect multi-valued fields across all rows
        List<ScheduleEvent> scheduleEvents = new ArrayList<>();
        Set<String> genreIds = new LinkedHashSet<>();
        AgeRating ageRating = null;
        Set<ContentRating> contentRatings = new LinkedHashSet<>();

        for (QuerySolution row : rows) {
            toScheduleEvent(row).ifPresent(scheduleEvents::add);

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
        }

        builder.scheduleEvents(scheduleEvents.toArray(new ScheduleEvent[0]));

        if (!genreIds.isEmpty()) {
            builder.genres(genreIds.stream().map(Genre::new).toList());
        }
        if (ageRating != null) {
            builder.ageRating(ageRating);
        }
        if (!contentRatings.isEmpty()) {
            builder.contentRatings(contentRatings.toArray(new ContentRating[0]));
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
        try {
            return Optional.of(ContentRating.valueOf(value.trim().toUpperCase()));
        } catch (IllegalArgumentException e) {
            log.severe("Unknown content rating value '%s', ignoring".formatted( value));
            return Optional.empty();
        }
    }


    public Optional<ScheduleEvent> toScheduleEvent(QuerySolution row) {
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
        if ("BVN".equals(name)) {
            return Optional.of(Channel.BVNT);
        }
        for (Channel channel : Channel.values()) {
            if (channel.getDisplayName().equalsIgnoreCase(name)) {
                return Optional.of(channel);
            }
        }
        log.severe("No channel found with name " + name);
        return Optional.empty();
    }

    protected void setString(String field, QuerySolution item, Consumer<String> consumer) {
        set(field, item, consumer, Literal::getString);
    }

    protected void setInstant(String field, QuerySolution item, Consumer<Instant> consumer) {
        set(field, item, consumer, lit -> ((XSDDateTime) lit.getValue()).asCalendar().toInstant());
    }


    protected <T> void set(String field, QuerySolution item, Consumer<T> consumer, Function<Literal, T> converter) {
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
