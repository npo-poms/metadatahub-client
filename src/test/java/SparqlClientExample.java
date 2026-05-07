import java.util.logging.Logger;
import nl.npo.metadatahub.client.Configuration;
import nl.npo.metadatahub.client.auth.*;
import nl.npo.metadatahub.client.sparql.MetadataSparqlClient;
import nl.npo.metadatahub.poms.Mapper;
import org.apache.jena.query.ResultSet;


final Logger log = Logger.getLogger(OAuth2Config.class.getName());

/**
 * Example: How to use the MetadataHub SPARQL client.
 * This shows how to use the client standalone, without Spring.
 */

void main() throws Exception {
    System.setProperty("log4j2.root.level","INFO");

    var configuration = new Configuration();
    var executor = configuration.createClient();


    firstQuery(executor);
  /*  // Example 1: SELECT query
    selectExample(executor);

    // Example 2: CONSTRUCT query
    constructExample(executor);

    // Example 3: ASK query
    askExample(executor);*/
}

private static void firstQuery(MetadataSparqlClient client) throws Exception {
    String firstQuery = """
        PREFIX ec: <http://www.ebu.ch/metadata/ontologies/ebucoreplus#>
        PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
    SELECT ?title ?description ?dateCreated ?prid
    WHERE {
      ?entity ec:hasIdentifier ?id .
      ?id ec:identifierValue "VPWON_1257874" .
      ?id ec:identifierValue ?prid .
      OPTIONAL { ?entity ec:title ?title . }
      OPTIONAL { ?entity ec:name ?prid . }
      OPTIONAL { ?entity ec:contentDescription ?description . }
      OPTIONAL { ?entity ec:hasDateCreated ?dateCreated . }
      
    }
    LIMIT 1000""";

    ResultSet result = client.selectQuery(firstQuery);
    List<String> fields = result.getResultVars();
    Mapper mapper = new Mapper(fields);
    while(result.hasNext()) {
        var row = result.next();
        IO.println(mapper.toProgram(row));
        for (String field : fields) {
            IO.println(field + "\t" + row.get(field).asLiteral().getString());
        }
    }
}


