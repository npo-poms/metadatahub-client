package nl.npo.metadatahub.client.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for MetadataHub SPARQL client and OAuth authentication.
 */
@Component
@ConfigurationProperties(prefix = "metadatahub")
public class MetadataHubProperties {

    private String sparqlEndpoint;
    private Oauth2 oauth2 = new Oauth2();
    private Client client = new Client();

    public String getSparqlEndpoint() {
        return sparqlEndpoint;
    }

    public void setSparqlEndpoint(String sparqlEndpoint) {
        this.sparqlEndpoint = sparqlEndpoint;
    }

    public Oauth2 getOauth2() {
        return oauth2;
    }

    public void setOauth2(Oauth2 oauth2) {
        this.oauth2 = oauth2;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    /**
     * OAuth2 configuration
     */
    public static class Oauth2 {
        private String tokenUri;
        private String clientId;
        private String clientSecret;
        private String scope = "openid profile email";

        public String getTokenUri() {
            return tokenUri;
        }

        public void setTokenUri(String tokenUri) {
            this.tokenUri = tokenUri;
        }

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getClientSecret() {
            return clientSecret;
        }

        public void setClientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
        }

        public String getScope() {
            return scope;
        }

        public void setScope(String scope) {
            this.scope = scope;
        }
    }

    /**
     * HTTP client configuration
     */
    public static class Client {
        private long connectTimeoutMs = 10000;
        private long readTimeoutMs = 30000;
        private long writeTimeoutMs = 30000;

        public long getConnectTimeoutMs() {
            return connectTimeoutMs;
        }

        public void setConnectTimeoutMs(long connectTimeoutMs) {
            this.connectTimeoutMs = connectTimeoutMs;
        }

        public long getReadTimeoutMs() {
            return readTimeoutMs;
        }

        public void setReadTimeoutMs(long readTimeoutMs) {
            this.readTimeoutMs = readTimeoutMs;
        }

        public long getWriteTimeoutMs() {
            return writeTimeoutMs;
        }

        public void setWriteTimeoutMs(long writeTimeoutMs) {
            this.writeTimeoutMs = writeTimeoutMs;
        }
    }
}

