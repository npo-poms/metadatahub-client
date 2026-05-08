import jakarta.xml.bind.JAXB;
import java.util.List;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.logging.Logger;
import nl.npo.metadatahub.client.Configuration;
import nl.npo.metadatahub.client.auth.*;
import nl.npo.metadatahub.client.sparql.MetadataSparqlClient;
import nl.npo.metadatahub.poms.Mapper;
import nl.npo.metadatahub.poms.MetadataToPoms;
import nl.vpro.domain.media.Channel;
import org.apache.jena.query.QuerySolution;
import nl.vpro.domain.media.Program;
import org.apache.jena.query.ResultSet;


static final Logger log = Logger.getLogger("main");

/**
 * Example: How to use the MetadataHub SPARQL client.
 * This shows how to use the client standalone, without Spring.
 */

void main() throws Exception {
    System.setProperty("log4j2.root.level","INFO");

    var configuration = new Configuration();
    var client = configuration.createClient();
    var poms = new MetadataToPoms(client);

    //JAXB.marshal(poms.getProgram( "POW_05977062"), System.out);

    IO.println(poms.getScheduleEevents(Channel.NED1, LocalDate.of(2026, 3, 1)));
}
