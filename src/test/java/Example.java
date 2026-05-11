import jakarta.xml.bind.JAXB;
import static java.lang.ScopedValue.where;
import static java.nio.file.Files.newOutputStream;
import java.util.logging.Logger;
import javax.xml.transform.stream.StreamResult;
import nl.npo.metadatahub.client.Configuration;
import static nl.npo.metadatahub.client.sparql.MetadataSparqlClient.onQueryExecuted;
import nl.npo.metadatahub.poms.*;
import nl.vpro.api.client.frontend.NpoApiClients;
import nl.vpro.domain.media.*;
import nl.vpro.logging.log4j2.CaptureToSimpleLogger;
import nl.vpro.logging.simple.OutputStreamSimpleLogger;
import nl.vpro.media.tva.Transform;
import nl.vpro.util.Env;
import nl.vpro.util.ThreadPools;
import static org.apache.jena.query.ResultSetFormatter.*;
import org.apache.logging.log4j.jul.Log4jBridgeHandler;

/**
 * - Gets the programs from 1 day
 *    (currently via poms api, I could not yet get it working via sparql)
 * - For every program
 *   - obtain it via sparql
 *   - map it to poms objects
 *   - write the sparql response to file (<mid>.json), the xml from poms to file (<mid>.poms.xml), and the mapped xml (<mid>.mh.xml)
 *
 * This is to compare the results from poms and sparql
 */

void main() throws Exception {
    Log4jBridgeHandler.install(true, null, true);
    System.setProperty("log4j2.root.level","INFO");
    Logger log = Logger.getLogger("main");

    var resultsPrent = Paths.get("results");
    try(
        var pomsServices = NpoApiClients.configured(Env.ACC).build();
        var metadataHubMediaService = new MetadataHubService(new Configuration().createClient());
        ) {
        for (Day day : List.of(
            new Day(Channel.NED2, LocalDate.of(2026, 4, 1))
            //new Day(Channel.NED1, LocalDate.of(2026, 3, 1)),
            //new Day(Channel.RAD1, LocalDate.of(2026, 4, 1))
        )) {

            // todo, I couldn't get this working yet via MH
            var resultFromPoms = pomsServices.getScheduleService().listChannel(day.channel.name(), day.day, null, null, "all", null, 0L, 240);

            Path dayDir = resultsPrent.resolve(day.day().toString());
            Path results = dayDir.resolve(day.channel.name());
            Files.createDirectories(results);
            MediaTable pomsMediaTable = new MediaTable();
            MediaTable mhMediaTable = new MediaTable();
            Schedule schedule = new Schedule();
            for (var event : resultFromPoms) {
                schedule.addScheduleEvent(event);;
                var mo = event.getParent();

                var mid = mo.getMid();

                var mhJsonFile = results.resolve(mid + ".mh.json");
                var mhFile = results.resolve(mid + ".mh.xml");
                var pomsFile = results.resolve(mid + ".poms.xml");

                try (
                    var logfile = newOutputStream(results.resolve(mid + ".log"));
                    var capture = CaptureToSimpleLogger.of(
                        OutputStreamSimpleLogger.builder().output(logfile).build()
                    );
                    var jsout = newOutputStream(mhJsonFile);
                    var pomsout = newOutputStream(pomsFile);
                    var out = newOutputStream(mhFile)) {


                    JAXB.marshal(mo, pomsout);
                    log.info("POMS: " + pomsFile);


                    where(onQueryExecuted,
                        (rs) -> {

                            outputAsJSON(jsout, rs);
                            log.info("Sparql response: " + mhJsonFile);
                        }
                    ).run(() -> {
                        try {
                            metadataHubMediaService.getProgram(mo.getMid()).ifPresentOrElse(
                                program -> {
                                    JAXB.marshal(program, out);
                                    mhMediaTable.add(program);
                                    log.info("MH: " + mhFile);
                                },
                                () -> log.info("MH: no program found for mid " + mo.getMid())
                            );
                        } catch (Exception e) {
                            log.info("MH: error fetching program for mid " + mo.getMid() + ": " + e.getMessage());
                        }
                    });
                }
            }
            pomsMediaTable.fillFrom(schedule);
            try (var tvaFile = newOutputStream(dayDir.resolve(day.channel.name() + ".tva.xml"))) {
                Transform.toTVA(pomsMediaTable, new StreamResult(tvaFile));
            }

        }

    }
    log.info("ready");
    ThreadPools.shutdown();
    // fails
    //IO.println(poms.getScheduleEevents(Channel.NED1, LocalDate.of(2026, 3, 1)));
}

record Day(Channel channel, LocalDate day) {
}
