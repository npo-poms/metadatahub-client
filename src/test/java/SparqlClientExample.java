import java.util.logging.Logger;
import nl.npo.metadatahub.client.auth.*;
import nl.npo.metadatahub.client.sparql.MetadataSparqlClient;
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
    SELECT ?title ?description ?dateCreated
    WHERE {
      ?entity ec:hasIdentifier ?id .
      ?id ec:identifierValue "VPWON_1257874" .
      ?id ec:name "PRID" .
      OPTIONAL { ?entity ec:title ?title . }
      OPTIONAL { ?entity ec:contentDescription ?description . }
      OPTIONAL { ?entity ec:hasDateCreated ?dateCreated . }
    }
    LIMIT 1""";

    ResultSet result = client.selectQuery(firstQuery);
    result.getResultVars();
    while(result.hasNext()) {
        var row = result.next();
        System.out.println("Title: " + row.get("title"));
        System.out.println("Description: " + row.get("description"));
        System.out.println("Date Created: " + row.get("dateCreated"));
    }
}
