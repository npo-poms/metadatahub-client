package nl.npo.metadatahub.client.sparql;

import java.io.*;
import lombok.extern.log4j.Log4j2;
import org.apache.jena.query.*;
import nl.npo.metadatahub.client.auth.TokenManager;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

/**
 * HTTP client for executing authenticated SPARQL queries against the MetadataHub endpoint.
 * Pure Java implementation using java.net.http.HttpClient.
 */

@Log4j2
public class MetadataSparqlClient {

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
    public MetadataSparqlClient(TokenManager tokenManager, SparqlConfig config) {
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
    public MetadataSparqlClient(HttpClient httpClient, TokenManager tokenManager, SparqlConfig config) {
        this.httpClient = httpClient;
        this.tokenManager = tokenManager;
        this.config = config;
    }
    /**
     * Execute a SPARQL SELECT query.
     *
     * @param sparqlQuery the SPARQL query string
     * @return parsed SPARQL result set
     */
    public ResultSet selectQuery(String sparqlQuery) throws TokenManager.TokenException, IOException, InterruptedException {
        log.debug("Executing SPARQL SELECT query");
        return sendQuery(sparqlQuery, "application/sparql-results+json");
    }


    /**
     * Send the query to the SPARQL endpoint with Bearer token authentication using GET with query parameter.
     * Matches behavior of: curl -G --data-urlencode 'query=...'
     */
    private ResultSet sendQuery(String sparqlQuery, String acceptHeader) throws TokenManager.TokenException, IOException, InterruptedException {
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

        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());



        if (response.statusCode() == 401) {
            throw new TokenManager.TokenException("Unauthorized");
        } else if (response.statusCode() < 200 || response.statusCode() >= 300) {
            byte[] b = response.body().readAllBytes();
            throw new IllegalStateException("SPARQL endpoint returned status " + response.statusCode() + ": " +  new String(b, StandardCharsets.UTF_8));
        }

        ResultSet results = ResultSetFactory.fromJSON(response.body());



        return results;
    }





}
