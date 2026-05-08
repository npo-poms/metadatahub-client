package nl.npo.metadatahub.poms;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import lombok.SneakyThrows;
import lombok.extern.java.Log;
import nl.npo.metadatahub.client.sparql.MetadataSparqlClient;
import nl.vpro.domain.media.*;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;

@Log
public class MetadataToPoms {

    private final MetadataSparqlClient client;

    public MetadataToPoms(MetadataSparqlClient client) {
        this.client = client;
    }

    @SneakyThrows
    public Program getProgram(String mid) {
        String template = readQueryTemplate("mediaobject.sparql");
        String query = template.formatted(mid);
        ResultSet resultSet = client.selectQuery(query);
        List<String> vars = resultSet.getResultVars();
        List<QuerySolution> rows = new ArrayList<>();
        while (resultSet.hasNext()) {
            rows.add(resultSet.next());
        }
        if (rows.isEmpty()) {
            return null;
        }
        var builder = MediaBuilder.broadcast().mid(mid);
        new Mapper(vars).toProgram(rows, builder);
        return builder.build();
    }

    @SneakyThrows
    public List<ScheduleEvent> getScheduleEevents(Channel channel, LocalDate day) {
        String template = readQueryTemplate("query_by_day_and_channel.sparql");
        String query = template.formatted(
            day.atTime(Schedule.START_OF_SCHEDULE).toString(),
            day.plusDays(1).atTime(Schedule.START_OF_SCHEDULE).toString(),
            channel.getDisplayName()
            );
        ;
        ResultSet resultSet = client.selectQuery(query);
        var mapper = new Mapper(resultSet.getResultVars());
        List<ScheduleEvent> events = new ArrayList<>();

        while (resultSet.hasNext()) {
            mapper.toScheduleEvent(resultSet.next()).ifPresent(events::add);
        }

        return events;
    }

    @SneakyThrows
    private static String readQueryTemplate(String fileName) {
        try (var in = Objects.requireNonNull(
            MetadataToPoms.class.getResourceAsStream("/sparql/" + fileName),
            "Missing resource /sparql/" + fileName
        )) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
