package nl.npo.metadatahub.client;

import java.io.*;
import java.util.function.BiConsumer;

import lombok.extern.java.Log;
import nl.vpro.domain.user.Editor;
import org.apache.jena.query.*;
import nl.npo.metadatahub.client.auth.TokenManager;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

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

@Log
public class MetadatahubClient implements AutoCloseable {

    public static final ScopedValue<BiConsumer<String, ResultSet>> onQueryExecuted = ScopedValue.newInstance();

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final HttpClient httpClient;
    private final TokenManager tokenManager;
    private final SparqlConfig config;
    private final EditorialConfig editorialConfig;

    /**
     * Create a SPARQL HTTP client.
     *
     * @param tokenManager OAuth2 token manager
     * @param config SPARQL endpoint configuration
     */
    public MetadatahubClient(TokenManager tokenManager, SparqlConfig config, EditorialConfig editorialConfig) {
        this(HttpClient.newBuilder()
            .connectTimeout(config.connectTimeout())
            .build(), tokenManager, config, editorialConfig);
    }

    /**
     * Create a SPARQL HTTP client with custom HttpClient.
     *
     * @param httpClient custom HttpClient
     * @param tokenManager OAuth2 token manager
     * @param config SPARQL endpoint configuration
     */
    public MetadatahubClient(HttpClient httpClient, TokenManager tokenManager, SparqlConfig config, EditorialConfig editorialConfig) {
        this.httpClient = httpClient;
        this.tokenManager = tokenManager;
        this.config = config;
        this.editorialConfig = editorialConfig;
    }
    /**
     * Execute a SPARQL SELECT query.
     *
     * @param sparqlQuery the SPARQL query string
     * @return parsed SPARQL result set
     */
    public ResultSet selectQuery(String sparqlQuery) throws TokenManager.TokenException, IOException, InterruptedException {
        log.fine(() -> "Executing SPARQL SELECT query \n%s".formatted(sparqlQuery));

        return sendQuery(sparqlQuery, "application/sparql-results+json");
    }
    ObjectMapper mapper = new ObjectMapper();

    public JsonNode getByPrid(String prid) throws TokenManager.TokenException, IOException, InterruptedException {
        String token = tokenManager.getAccessToken();

        URI uri = URI.create(editorialConfig.endpoint() + "?prid=" + URLEncoder.encode(prid, StandardCharsets.UTF_8));
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(uri)
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token)
                .header("Accept", "application/json")
                .timeout(editorialConfig.readTimeout())
                .build();
        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

        return mapper.readTree(response.body());

    }


    @Override
    public void close() {
        this.httpClient.close();
    }


    /**
     * Send the query to the SPARQL endpoint with Bearer token authentication using GET with query parameter.
     * Matches behavior of: curl -G --data-urlencode 'query=...'
     */
    private ResultSet sendQuery(String sparqlQuery, String acceptHeader) throws TokenManager.TokenException, IOException, InterruptedException {
        String token = tokenManager.getAccessToken();
        String encodedQuery = URLEncoder.encode(sparqlQuery, StandardCharsets.UTF_8);
        String endpoint = config.endpoint() + "?query=" + encodedQuery;

        log.fine(() -> "Sending SPARQL query to: %s".formatted(config.endpoint()));

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
            throw new IllegalStateException("SPARQL endpoint returned status " + response.statusCode() + ": " +  new String(b, StandardCharsets.UTF_8) + " :" + sparqlQuery);
        }


        ResultSet results = ResultSetFactory.fromJSON(response.body());
        if (onQueryExecuted.isBound()) {
            // store the result in bytearray, because we need to consume it twice.
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ResultSetFormatter.outputAsJSON(out, results);

            // copy to return
            results = ResultSetFactory.fromJSON(new ByteArrayInputStream(out.toByteArray()));
            // copy to consume
            onQueryExecuted.get().accept(sparqlQuery, ResultSetFactory.fromJSON(new ByteArrayInputStream(out.toByteArray())));
        }

        return results;
    }





}
