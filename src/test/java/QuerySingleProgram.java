
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

        metadataHubMediaService.getProgram("AVRO_1353119").ifPresent(
            program -> {
                JAXB.marshal(program, System.out);

            });

        metadataHubMediaService.getProgram("POMS_AT_10136616").ifPresent(
            program -> {
                JAXB.marshal(program, System.out);

            });

    }
    log.info("ready");
    ThreadPools.shutdown();
    // fails
    //IO.println(poms.getScheduleEevents(Channel.NED1, LocalDate.of(2026, 3, 1)));
}

