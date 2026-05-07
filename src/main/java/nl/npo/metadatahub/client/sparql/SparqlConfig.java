package nl.npo.metadatahub.client.sparql;

import java.time.Duration;

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
        if (endpoint == null) throw new IllegalArgumentException("endpoint cannot be null");
        if (connectTimeout == null) connectTimeout = Duration.ofSeconds(10);
        if (readTimeout == null) readTimeout = Duration.ofSeconds(30);
    }

    /**
     * Create with defaults for timeouts.
     */
    public SparqlConfig(String endpoint) {
        this(endpoint, Duration.ofSeconds(10), Duration.ofSeconds(30));
    }
}

