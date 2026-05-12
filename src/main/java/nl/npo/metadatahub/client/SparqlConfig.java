package nl.npo.metadatahub.client;

import java.time.Duration;
import static java.util.Objects.requireNonNull;

/**
 * SPARQL client configuration.
 */
public record SparqlConfig(
    String endpoint,
    Duration connectTimeout,
    Duration readTimeout
) {
    /**
     * Compact constructor with defaults.
     */
    public SparqlConfig {
        requireNonNull(endpoint, "endpoint cannot be null");
        if (connectTimeout == null) connectTimeout = Duration.ofSeconds(10);
        if (readTimeout == null) readTimeout = Duration.ofSeconds(30);
    }

    /**
     * Create with defaults for timeouts.
     */
    public SparqlConfig(String endpoint) {
        this(endpoint, null, null);
    }
}

