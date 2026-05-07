package nl.npo.metadatahub.client.sparql;

import lombok.extern.log4j.Log4j2;
import org.apache.jena.query.*;
import nl.npo.metadatahub.client.auth.TokenManager;
import nl.npo.metadatahub.client.sparql.model.SparqlResult;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HTTP client for executing authenticated SPARQL queries against the MetadataHub endpoint.
 * Pure Java implementation using java.net.http.HttpClient.
 */

@Log4j2
public class SparqlHttpClient {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final HttpClient httpClient;
    private final TokenManager tokenManager;
    private final SparqlConfig config;

    /**
     * Create a SPARQL HTTP client.
     *
     * @param tokenManager OAuth2 token manager
     * @param config SPARQL endpoint configuration
     */
    public SparqlHttpClient(TokenManager tokenManager, SparqlConfig config) {
        this(HttpClient.newBuilder()
            .connectTimeout(config.connectTimeout())
            .build(), tokenManager, config);
    }

    /**
     * Create a SPARQL HTTP client with custom HttpClient.
     *
     * @param httpClient custom HttpClient
     * @param tokenManager OAuth2 token manager
     * @param config SPARQL endpoint configuration
     */
    public SparqlHttpClient(HttpClient httpClient, TokenManager tokenManager, SparqlConfig config) {
        this.httpClient = httpClient;
        this.tokenManager = tokenManager;
        this.config = config;
    }
    /**
     * Execute a SPARQL SELECT query.
     *
     * @param sparqlQuery the SPARQL query string
     * @return parsed SPARQL result set
     * @throws SparqlException if the query fails
     */
    public SparqlResult selectQuery(String sparqlQuery) throws SparqlException {
        log.debug("Executing SPARQL SELECT query");
        return executeQuery(sparqlQuery, "application/sparql-results+json");
    }

    /**
     * Execute a SPARQL CONSTRUCT query.
     *
     * @param sparqlQuery the SPARQL query string
     * @return RDF results as SPARQL result (JSON-LD format)
     * @throws SparqlException if the query fails
     */
    public SparqlResult constructQuery(String sparqlQuery) throws SparqlException {
        log.debug("Executing SPARQL CONSTRUCT query");
        return executeQuery(sparqlQuery, "application/ld+json");
    }

    /**
     * Execute a SPARQL ASK query.
     *
     * @param sparqlQuery the SPARQL query string
     * @return true or false as a single result
     * @throws SparqlException if the query fails
     */
    public boolean askQuery(String sparqlQuery) throws SparqlException {
        log.debug("Executing SPARQL ASK query");
        try {
            String response = sendQuery(sparqlQuery, "application/sparql-results+json");
            return response.contains("\"boolean\":true") || response.contains("true");
        } catch (Exception e) {
            throw new SparqlException("ASK query failed: " + e.getMessage(), e);
        }
    }

    /**
     * Execute a SPARQL query and return results.
     */
    private SparqlResult executeQuery(String sparqlQuery, String acceptHeader) throws SparqlException {
        try {
            String response = sendQuery(sparqlQuery, acceptHeader);
            return parseJsonResult(response);
        } catch (TokenManager.TokenException e) {
            log.warn("Unauthorized (401): Invalidating token and retrying");
            tokenManager.invalidateToken();
            try {
                String response = sendQuery(sparqlQuery, acceptHeader);
                return parseJsonResult(response);
            } catch (Exception retryException) {
                throw new SparqlException("Query failed after token refresh: " + retryException.getMessage(), retryException);
            }
        } catch (Exception e) {
            throw new SparqlException("SPARQL query execution failed: " + e.getMessage(), e);
        }
    }

    /**
     * Send the query to the SPARQL endpoint with Bearer token authentication using GET with query parameter.
     * Matches behavior of: curl -G --data-urlencode 'query=...'
     */
    private String sendQuery(String sparqlQuery, String acceptHeader) throws Exception {
        String token = tokenManager.getAccessToken();
        String encodedQuery = URLEncoder.encode(sparqlQuery, StandardCharsets.UTF_8);
        String endpoint = config.endpoint() + "?query=" + encodedQuery;

        log.debug("Sending SPARQL query to: {}", config.endpoint());

        HttpRequest request = HttpRequest.newBuilder()
            .GET()
            .uri(URI.create(endpoint))
            .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token)
            .header("Accept", acceptHeader)
            .timeout(config.readTimeout())
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 401) {
            throw new TokenManager.TokenException("Unauthorized");
        } else if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new SparqlException("SPARQL endpoint returned status " + response.statusCode() + ": " + response.body());
        }

        return response.body();
    }


    /**
     * Parse SPARQL JSON results using Apache Jena.
     */
    private SparqlResult parseJsonResult(String jsonResponse) throws Exception {
        try {
            ResultSet results = ResultSetFactory.fromJSON(
                new ByteArrayInputStream(jsonResponse.getBytes())
            );

            List<String> variables = results.getResultVars();
            List<Map<String, String>> resultList = new ArrayList<>();

            while (results.hasNext()) {
                var binding = results.next();
                Map<String, String> row = new HashMap<>();
                for (String var : variables) {
                    var value = binding.get(var);
                    if (value != null) {
                        row.put(var, value.toString());
                    }
                }
                resultList.add(row);
            }

            return new SparqlResult(variables, resultList);
        } catch (Exception e) {
            log.error("Failed to parse SPARQL JSON result", e);
            throw new SparqlException("Failed to parse SPARQL result: " + e.getMessage(), e);
        }
    }

    /**
     * Exception for SPARQL-related errors.
     */
    public static class SparqlException extends Exception {
        public SparqlException(String message) {
            super(message);
        }

        public SparqlException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
