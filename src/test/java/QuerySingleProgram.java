
import jakarta.xml.bind.JAXB;
import java.util.logging.Logger;
import nl.npo.metadatahub.client.Configuration;
import nl.npo.metadatahub.poms.*;
import nl.vpro.api.client.frontend.NpoApiClients;
import nl.vpro.domain.media.*;
import nl.vpro.util.Env;
import nl.vpro.util.ThreadPools;
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

        List<Segment> segment = metadataHubMediaService.getSegments("AVRO_1353119");
        for (Segment seg : segment) {
            JAXB.marshal(seg, System.out);
        }

    }
    log.info("ready");
    ThreadPools.shutdown();
    // fails
    //IO.println(poms.getScheduleEevents(Channel.NED1, LocalDate.of(2026, 3, 1)));
}

