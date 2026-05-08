package nl.npo.metadatahub.poms;

import com.google.common.cache.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.java.Log;
import nl.npo.metadatahub.client.sparql.MetadataSparqlClient;
import nl.vpro.domain.classification.*;
import nl.vpro.domain.media.*;
import nl.vpro.domain.user.BroadcasterService;
import nl.vpro.domain.user.ServiceLocator;
import nl.vpro.media.broadcaster.URLBroadcasterServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.checkerframework.checker.nullness.qual.NonNull;

@Log
public class MetadataHubMediaService implements MediaProvider, AutoCloseable {


    static MediaClassificationService classificationService = MediaClassificationService.getInstance();
    static BroadcasterService broadcasterService = new URLBroadcasterServiceImpl("https://poms.omroep.nl/broadcasters/");
    static {
        ClassificationServiceLocator.setInstance(classificationService);
        ServiceLocator.setBroadcasterService(broadcasterService);
    }

    private final MetadataSparqlClient client;

    Locale nl_vpp = Locale.of("nl", "", "vpp");

    /**
     * Ad hoc logic to properly try to map genre?
     */
    @Getter
    private final LoadingCache<String, Optional<Genre>> genreCache = CacheBuilder.newBuilder()
        .build(new CacheLoader<>() {
            @Override
            public @NonNull Optional<Genre> load(final @NonNull String key) {
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

                if (!primaryTerm.isPresent()) {
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
        });

    public MetadataHubMediaService(MetadataSparqlClient client) {
        this.client = client;
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
        List<String> vars = resultSet.getResultVars();
        List<QuerySolution> rows = new ArrayList<>();
        while (resultSet.hasNext()) {
            rows.add(resultSet.next());
        }
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        var builder = MediaBuilder.broadcast().mid(mid);
        new Mapper(this, vars).toProgram(rows, builder);
        return Optional.of(builder.build());
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
        var mapper = new Mapper(this,resultSet.getResultVars());
        List<ScheduleEvent> events = new ArrayList<>();

        while (resultSet.hasNext()) {
            mapper.toScheduleEvent(resultSet.next()).ifPresent(events::add);
        }

        return events;
    }

    @Override
    public void close() throws Exception {
        client.close();
        classificationService.close();
    }

    @SneakyThrows
    private static String readQueryTemplate(String fileName) {
        try (var in = Objects.requireNonNull(
            MetadataHubMediaService.class.getResourceAsStream("/sparql/" + fileName),
            "Missing resource /sparql/" + fileName
        )) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
