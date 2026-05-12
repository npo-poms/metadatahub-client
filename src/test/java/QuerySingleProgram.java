
import jakarta.xml.bind.JAXB;
import static java.lang.ScopedValue.where;
import java.util.logging.Logger;
import nl.npo.metadatahub.client.Configuration;

import static java.nio.file.Files.newOutputStream;
import static nl.npo.metadatahub.client.MetadatahubClient.onQueryExecuted;

import nl.npo.metadatahub.client.auth.TokenManager;
import nl.npo.metadatahub.poms.*;
import nl.vpro.api.client.frontend.NpoApiClients;
import nl.vpro.domain.media.*;
import nl.vpro.jackson2.Jackson2Mapper;
import nl.vpro.util.Env;
import nl.vpro.util.ThreadPools;
import static org.apache.jena.query.ResultSetFormatter.outputAsJSON;
import static tools.jackson.databind.SerializationFeature.INDENT_OUTPUT;

import org.apache.logging.log4j.jul.Log4jBridgeHandler;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 */

void main() throws Exception {
    Log4jBridgeHandler.install(true, null, true);
    System.setProperty("log4j2.root.level","INFO");
    Logger log = Logger.getLogger("main");
    ObjectMapper mapper = JsonMapper.builder().enable(INDENT_OUTPUT).build();

    String mid = "PREPR_NOS_16583295";
    var results = Paths.get("results").resolve("single");
    var mhEditorialFile = results.resolve(mid + ".mh.editorial.json");
    var mhFile = results.resolve(mid + ".mh.xml");
    try(
            var metadataHubMediaService = new MetadataHubService(new Configuration().createClient());
            var editorialOut = newOutputStream(mhEditorialFile);
            var out = newOutputStream(mhFile)
        ) {

        where(onQueryExecuted,
            (query, rs) -> {
                log.info(query);
                log.info("Sparql response: " + rs);
            }).run(() -> {

            try {
                JsonNode node  = metadataHubMediaService.getClient().getByPrid(mid);
                mapper.writeValue(editorialOut, node);
            } catch (TokenManager.TokenException | InterruptedException | IOException e) {
                throw new RuntimeException(e);
            }
            Optional<Program> program = metadataHubMediaService.getProgram(mid);

            program.ifPresent(p -> {
                JAXB.marshal(p, out);
            });
        });
    }
    log.info("ready");
    ThreadPools.shutdown();
    // fails
    //IO.println(poms.getScheduleEevents(Channel.NED1, LocalDate.of(2026, 3, 1)));
}

