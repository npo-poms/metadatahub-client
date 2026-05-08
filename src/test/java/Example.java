import jakarta.xml.bind.JAXB;
import nl.npo.metadatahub.client.Configuration;
import nl.npo.metadatahub.client.sparql.MetadataSparqlClient;
import nl.npo.metadatahub.poms.*;
import nl.vpro.api.client.frontend.NpoApiClients;
import nl.vpro.domain.media.Channel;
import nl.vpro.logging.log4j2.CaptureToSimpleLogger;
import nl.vpro.logging.simple.OutputStreamSimpleLogger;
import nl.vpro.util.Env;
import org.apache.jena.query.ResultSetFormatter;

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
    System.setProperty("log4j2.root.level","INFO");

    var pomsServices = NpoApiClients.configured(Env.PROD).build();
    var metadataHubMediaService = new MetadataHubMediaService(new Configuration().createClient());


    Channel channel = Channel.NED1;
    LocalDate day = LocalDate.of(2026, 3, 1);

    // todo, I couldn get this working yet via MH
    var resultFromPoms = pomsServices.getScheduleService().listChannel(Channel.NED1.name(), day, null, null, "all", null,0L, 240);

    Path results = Paths.get("results").resolve(day.toString()).resolve(channel.name());
    Files.createDirectories(results);

    for (var event : resultFromPoms) {
        var mo = event.getParent();
        var mid = mo.getMid();
        var pomsFile = results.resolve(mid + ".poms.xml");
        try (FileOutputStream out = new FileOutputStream(pomsFile.toFile())) {
            JAXB.marshal(mo, out);
            IO.println("POMS: " + pomsFile.toAbsolutePath());
        }
        var mhFile = results.resolve(mid + ".mh.xml");
        var mhJsonFile = results.resolve(mid + ".mh.json");

        try (
            FileOutputStream log = new FileOutputStream(results.resolve(mid + ".log").toFile());
            CaptureToSimpleLogger capture = CaptureToSimpleLogger.of(OutputStreamSimpleLogger.builder()
                .output(log).build());
            FileOutputStream jsout = new FileOutputStream(results.resolve(mid + ".mh.json").toFile());
            FileOutputStream out = new FileOutputStream(results.resolve(mid + ".mh.xml").toFile())) {
            ScopedValue.where(MetadataSparqlClient.onQueryExecuted, (rs) -> {
                ResultSetFormatter.outputAsJSON(jsout, rs);
            }).run(() -> {
                try {
                    metadataHubMediaService.getProgram(mo.getMid()).ifPresentOrElse(
                        program -> {
                            JAXB.marshal(program, out);
                            IO.println("MH: " + mhFile.toAbsolutePath());
                        },
                        () -> IO.println("MH: no program found for mid " + mo.getMid())
                    );
                } catch (IllegalStateException e) {
                    IO.println("MH: error fetching program for mid " + mo.getMid() + ": " + e.getMessage());
                }

            });
        }
    }

    // fails
    //IO.println(poms.getScheduleEevents(Channel.NED1, LocalDate.of(2026, 3, 1)));
}
