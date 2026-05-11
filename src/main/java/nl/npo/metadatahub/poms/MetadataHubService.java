package nl.npo.metadatahub.poms;

import com.google.common.cache.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import lombok.SneakyThrows;
import lombok.extern.java.Log;
import nl.npo.metadatahub.client.sparql.MetadataSparqlClient;
import nl.vpro.domain.classification.*;
import nl.vpro.domain.media.*;
import nl.vpro.domain.user.BroadcasterService;
import nl.vpro.domain.user.ServiceLocator;
import nl.vpro.media.broadcaster.URLBroadcasterServiceImpl;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;

/**
 * A service the query poms data from the new MetadataHub sparql end points.
 * @see Mapper for actual mapping.
 */
@Log
public class MetadataHubService implements MediaProvider, AutoCloseable {


    static MediaClassificationService classificationService = MediaClassificationService.getInstance();
    static BroadcasterService broadcasterService = new URLBroadcasterServiceImpl("https://poms.omroep.nl/broadcasters/");
    static {
        ClassificationServiceLocator.setInstance(classificationService);
        ServiceLocator.setBroadcasterService(broadcasterService);
    }

    private final MetadataSparqlClient client;
    private final Mapper mapper;



    public MetadataHubService(MetadataSparqlClient client) {
        this.client = client;
        this.mapper = new Mapper();
    }


    @Override
    public <T extends MediaObject> T findByMid(boolean loadDeleted, String mid) {
        return (T) getProgram(mid).orElse(null);
    }


    @SneakyThrows
    public Optional<Program> getProgram(String mid) {
        String template = readQueryTemplate("mediaobject.sparql");
        String query = template.formatted(mid);
        ResultSet resultSet = client.selectQuery(query);
        if (resultSet.hasNext()) {
            QuerySolution next = resultSet.next();
            var builder = MediaBuilder.broadcast().mid(mid);
            mapper.toProgram(next, builder);
            getSegments(mid).forEach(builder::segments);

            return Optional.of(builder.build());
        } else {
            return Optional.empty();
        }
    }

    @SneakyThrows
    public List<ScheduleEvent> getScheduleEvents(Channel channel, LocalDate day) {
        String template = readQueryTemplate("query_by_day_and_channel.sparql");
        String query = template.formatted(
            day.atTime(Schedule.START_OF_SCHEDULE).toString(),
            day.plusDays(1).atTime(Schedule.START_OF_SCHEDULE).toString(),
            channel.getDisplayName()
        );
        ;
        ResultSet resultSet = client.selectQuery(query);
        List<ScheduleEvent> events = new ArrayList<>();

        while (resultSet.hasNext()) {
            mapper.toScheduleEvent(resultSet.getResultVars(), resultSet.next()).ifPresent(events::add);
        }

        return events;
    }


    @SneakyThrows
    public List<Segment> getSegments(String mid) {
        String template = readQueryTemplate("segments_of.sparql");
        String segmentsQuery = template.formatted(mid);
        ResultSet segmentsResult  = client.selectQuery(segmentsQuery);

        List<Segment> segments = new ArrayList<>();
        while (segmentsResult.hasNext()) {
            QuerySolution next = segmentsResult.next();
            String segment = next.getLiteral("prid").getString();
            MediaBuilder.SegmentBuilder builder = MediaBuilder.segment().mid(segment);
            mapper.toMediaObject(next, builder);
            segments.add(builder.build());

        }
        return segments;
    }
    @Override
    public void close() {
        client.close();
        classificationService.close();
    }

    @SneakyThrows
    private static String readQueryTemplate(String fileName) {
        try (var in = Objects.requireNonNull(
            MetadataHubService.class.getResourceAsStream("/sparql/" + fileName),
            "Missing resource /sparql/" + fileName
        )) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
