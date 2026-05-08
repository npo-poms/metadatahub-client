package nl.npo.metadatahub.poms;

import java.nio.charset.StandardCharsets;
import lombok.SneakyThrows;
import nl.npo.metadatahub.client.sparql.MetadataSparqlClient;
import nl.vpro.domain.media.Program;
import org.apache.jena.query.ResultSet;

public class MetadataToPoms {

    private final MetadataSparqlClient client;

    public MetadataToPoms(MetadataSparqlClient client) {
        this.client = client;
    }

    @SuppressWarnings("DataFlowIssue")
    @SneakyThrows
    public Program getProgram(MetadataSparqlClient client, String mid) {
        String query = new String(MetadataToPoms.class.getResourceAsStream("/sparql/mediaobject.sparql").readAllBytes(), StandardCharsets.UTF_8);
        String q = (query + """
                LIMIT 1
                """).formatted("?prid");
        ResultSet resultSet = client.selectQuery(q);
        Mapper mapper = new Mapper(resultSet.getResultVars());
        return mapper.toProgram(resultSet.next());
    }
}
