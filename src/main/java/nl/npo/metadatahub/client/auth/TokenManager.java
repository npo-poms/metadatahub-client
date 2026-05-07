package nl.npo.metadatahub.client.auth;

import lombok.extern.log4j.Log4j2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;
import tools.jackson.databind.ObjectMapper;

/**
 * Manages OAuth2 token acquisition, caching, and refresh.
 * Uses client credentials flow to obtain access tokens from Keycloak.
 * Pure Java implementation with no Spring dependencies.
 */

@Log4j2
public class TokenManager {

    private static final int REFRESH_BUFFER_SECONDS = 30; // Refresh token 30 seconds before expiry
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final HttpClient httpClient;
    private final OAuth2Config config;

    private String cachedToken;
    private Instant tokenExpirationTime;

    /**
     * Create a TokenManager with OAuth2 configuration.
     *
     * @param config OAuth2 configuration (token URI, client ID/secret, scope)
     */
    public TokenManager(OAuth2Config config) {
        this(HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build(), config);
    }

    /**
     * Create a TokenManager with custom HttpClient and OAuth2 configuration.
     *
     * @param httpClient custom HttpClient for requests
     * @param config OAuth2 configuration
     */
    public TokenManager(HttpClient httpClient, OAuth2Config config) {
        this.httpClient = httpClient;
        this.config = config;
    }

    /**
     * Get a valid OAuth2 access token, retrieving or refreshing as needed.
     *
     * @return access token string
     * @throws TokenException if token cannot be obtained
     */
    public synchronized String getAccessToken() throws TokenException {
        if (isTokenValid()) {
            log.debug("Using cached OAuth2 token");
            return cachedToken;
        }

        log.info("Requesting new OAuth2 token from: {}", config.tokenUri());
        try {
            return requestToken();
        } catch (Exception e) {
            log.error("Failed to obtain OAuth2 token", e);
            throw new TokenException("Failed to obtain access token: " + e.getMessage(), e);
        }
    }

    /**
     * Request a new access token using client credentials flow.
     */
    private String requestToken() throws Exception {
        // Build form body
        Map<String, String> formParams = Map.of(
            "grant_type", "client_credentials",
            "client_id", config.clientId(),
            "client_secret", config.clientSecret()
            //"scope", config.scope()
        );

        String body = formParams.entrySet().stream()
            .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
            .collect(Collectors.joining("&"));

        HttpRequest request = HttpRequest.newBuilder()
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .uri(URI.create(config.tokenUri()))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("Accept", "application/json")
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new TokenException("Token endpoint returned status " + response.statusCode() + ": " + response.body());
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> tokenResponse = objectMapper.readValue(response.body(), Map.class);

        Object tokenObj = tokenResponse.get("access_token");
        if (tokenObj == null) {
            throw new TokenException("No access_token in response: " + tokenResponse);
        }

        String token = tokenObj.toString();

        // Calculate expiration time
        Object expiresInObj = tokenResponse.get("expires_in");
        if (expiresInObj != null) {
            try {
                long expiresInSeconds = Long.parseLong(expiresInObj.toString());
                this.tokenExpirationTime = Instant.now().plusSeconds(expiresInSeconds - REFRESH_BUFFER_SECONDS);
                log.debug("Token will expire at: {}", tokenExpirationTime);
            } catch (NumberFormatException e) {
                log.warn("Could not parse expires_in from token response", e);
                this.tokenExpirationTime = Instant.now().plusSeconds(300); // Default to 5 minutes
            }
        } else {
            this.tokenExpirationTime = Instant.now().plusSeconds(300); // Default to 5 minutes
        }

        this.cachedToken = token;
        log.info("Successfully obtained new OAuth2 token");
        return token;
    }

    /**
     * Check if the cached token is still valid.
     */
    private boolean isTokenValid() {
        return cachedToken != null &&
               tokenExpirationTime != null &&
               Instant.now().isBefore(tokenExpirationTime);
    }

    /**
     * Invalidate the cached token, forcing a fresh token request on next call.
     */
    public synchronized void invalidateToken() {
        log.debug("Invalidating cached token");
        cachedToken = null;
        tokenExpirationTime = null;
    }

    /**
     * Exception for token-related errors.
     */
    public static class TokenException extends Exception {
        public TokenException(String message) {
            super(message);
        }

        public TokenException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
