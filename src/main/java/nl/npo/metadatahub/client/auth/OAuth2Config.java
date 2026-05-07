package nl.npo.metadatahub.client.auth;

/**
 * OAuth2 configuration for Keycloak authentication.
 * This is a record, so it automatically generates constructor, getters, equals, hashCode, and toString.
 */
public record OAuth2Config(
    String tokenUri,
    String clientId,
    String clientSecret,
    String scope
) {
    /**
     * Compact constructor with validation and default scope.
     */
    public OAuth2Config {
        if (tokenUri == null) throw new IllegalArgumentException("tokenUri cannot be null");
        if (clientId == null) throw new IllegalArgumentException("clientId cannot be null");
        if (clientSecret == null) throw new IllegalArgumentException("clientSecret cannot be null");
        if (scope == null) scope = "openid profile email";
    }
}

