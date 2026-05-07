package nl.npo.metadatahub.client.auth;

import static java.util.Objects.requireNonNull;
import java.util.Properties;
import lombok.NonNull;

/**
 
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

