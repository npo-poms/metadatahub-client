import java.util.logging.Logger;
import nl.npo.metadatahub.client.auth.*;
import nl.npo.metadatahub.client.sparql.SparqlQueryExecutor;
import nl.npo.metadatahub.client.sparql.model.SparqlResult;


final Logger log = Logger.getLogger(OAuth2Config.class.getName());

/**
 * Example: How to use the MetadataHub SPARQL client.
 * This shows how to use the client standalone, without Spring.
 */

void main() throws Exception {
    System.setProperty("log4j2.root.level","INFO");

    var configuration = new Configuration();
    var executor = configuration.getSparqlQueryExecutor();


    firstQuery(executor);
  /*  // Example 1: SELECT query
    selectExample(executor);

    // Example 2: CONSTRUCT query
    constructExample(executor);

    // Example 3: ASK query
    askExample(executor);*/
}

private static void firstQuery(SparqlQueryExecutor executor) throws Exception {
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

    SparqlResult result = executor.select(firstQuery);
    result.results().forEach(row -> {
        System.out.println("Title: " + row.get("title"));
        System.out.println("Description: " + row.get("description"));
        System.out.println("Date Created: " + row.get("dateCreated"));
    });

}
/**
 * Example: Execute a SELECT query to find resources
 */
private static void selectExample(SparqlQueryExecutor executor) throws Exception {
    String query = """
            PREFIX schema: <http://schema.org/>
            SELECT ?resource ?title
            WHERE {
                ?resource a schema:Thing ;
                         schema:name ?title .
            }
            LIMIT 100
            """;
        try {
            SparqlResult result = executor.select(query);
            System.out.println("SELECT Query Results:");
            System.out.println("  Variables: " + result.variables());
            System.out.println("  Result count: " + result.results().size());

            result.results().forEach(row ->
                System.out.println("    Resource: " + row.get("resource") +
                    " | Title: " + row.get("title"))
            );
        } catch (Exception e) {
            System.err.println("SELECT query failed: " + e.getMessage());
            e.printStackTrace();
        }
    }


    /**
     * Example: Execute a CONSTRUCT query to get RDF data
     */
    private static void constructExample(SparqlQueryExecutor executor) throws Exception {
        String query = """
            PREFIX schema: <http://schema.org/>
            CONSTRUCT {
                ?resource a schema:Thing ;
                         schema:name ?title ;
                         schema:description ?description .
            }
            WHERE {
                ?resource a schema:Thing ;
                         schema:name ?title ;
                         schema:description ?description .
            }
            LIMIT 50
            """;

        try {
            SparqlResult result = executor.construct(query);
            System.out.println("\nCONSTRUCT Query Results:");
            System.out.println("  Constructed triples: " + result.results().size());
        } catch (Exception e) {
            System.err.println("CONSTRUCT query failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Example: Execute an ASK query to check for data existence
     */
    private static void askExample(SparqlQueryExecutor executor) throws Exception {
        String query = """
            PREFIX schema: <http://schema.org/>
            ASK {
                ?resource a schema:Thing ;
                         schema:name ?title .
            }
            """;

        try {
            boolean exists = executor.ask(query);
            System.out.println("\nASK Query Results:");
            System.out.println("  Resources exist: " + exists);
        } catch (Exception e) {
            System.err.println("ASK query failed: " + e.getMessage());
            e.printStackTrace();
        }
    }


