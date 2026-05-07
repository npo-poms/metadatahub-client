package nl.npo.metadatahub.client;

import nl.npo.metadatahub.client.auth.OAuth2Config;
import nl.npo.metadatahub.client.auth.TokenManager;
import nl.npo.metadatahub.client.sparql.SparqlConfig;
import nl.npo.metadatahub.client.sparql.SparqlHttpClient;
import nl.npo.metadatahub.client.sparql.SparqlQueryExecutor;
import nl.npo.metadatahub.client.sparql.model.SparqlResult;

/**
 * Example: How to use the MetadataHub SPARQL client.
 * This shows how to use the client standalone, without Spring.
 */
public class SparqlClientExample {

    public static void main(String[] args) throws Exception {
        // Configure OAuth2
        OAuth2Config oauthConfig = new OAuth2Config(
            "https://auth.metadatahub.bijnpo.nl/realms/metadatahub/protocol/openid-connect/token",
            "your-client-id",
            "your-client-secret",
            "openid profile email"
        );

        // Create token manager
        TokenManager tokenManager = new TokenManager(oauthConfig);

        // Configure SPARQL endpoint
        SparqlConfig sparqlConfig = new SparqlConfig(
            "https://sparql.metadatahub.bijnpo.nl/sparql"
        );

        // Create SPARQL HTTP client
        SparqlHttpClient httpClient = new SparqlHttpClient(tokenManager, sparqlConfig);

        // Create executor (the main API)
        SparqlQueryExecutor executor = new SparqlQueryExecutor(httpClient);

        // Example 1: SELECT query
        selectExample(executor);

        // Example 2: CONSTRUCT query
        constructExample(executor);

        // Example 3: ASK query
        askExample(executor);
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
            System.out.println("  Variables: " + result.getVariables());
            System.out.println("  Result count: " + result.getResults().size());

            result.getResults().forEach(row ->
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
            System.out.println("  Constructed triples: " + result.getResults().size());
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

