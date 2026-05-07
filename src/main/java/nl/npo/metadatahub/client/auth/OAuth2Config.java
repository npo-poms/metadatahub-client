package nl.npo.metadatahub.client.auth;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import static java.util.Objects.requireNonNull;
import java.util.Properties;
import lombok.NonNull;
import org.apache.jena.vocabulary.OA;

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
        requireNonNull(tokenUri, "tokenUri cannot be null");
        requireNonNull(clientId, "clientId cannot be null");
        requireNonNull(clientSecret, "clientSecret cannot be null");
    }

    public static OAuth2Config fromProperties(@NonNull Properties props) {
        return new OAuth2Config(
            props.getProperty("sso.endpoint"),
            props.getProperty("client.id"),
            props.getProperty("client.secret"),
            null
        );
    }

}

