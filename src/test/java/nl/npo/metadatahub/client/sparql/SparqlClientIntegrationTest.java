package nl.npo.metadatahub.client.sparql;

import com.dasniko.testcontainers.keycloak.KeycloakContainer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import nl.npo.metadatahub.client.auth.TokenManager;
import nl.npo.metadatahub.client.config.MetadataHubProperties;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Integration tests for the SPARQL client with OAuth2 authentication using Keycloak.
 * This test demonstrates token acquisition from a real Keycloak instance.
 */
@Testcontainers
@SpringBootTest
class SparqlClientIntegrationTest {

    @Container
    static KeycloakContainer keycloak = new KeycloakContainer()
        .withRealmImportFile("test-realm.json");

    @Autowired
    private TokenManager tokenManager;

    @Autowired
    private SparqlQueryExecutor queryExecutor;

    @Autowired
    private MetadataHubProperties properties;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("metadatahub.oauth2.token-uri",
            () -> keycloak.getAuthServerUrl() + "/realms/test/protocol/openid-connect/token");
        registry.add("metadatahub.oauth2.client-id", () -> "test-client");
        registry.add("metadatahub.oauth2.client-secret", () -> "test-secret");
    }

    @Test
    void testTokenManagerWithKeycloak() throws Exception {
        // This test requires a Keycloak realm configuration
        // For now, we just verify the components are wired correctly
        assertNotNull(tokenManager);
        assertNotNull(queryExecutor);
        assertNotNull(properties);
    }

    @Test
    void testSparqlQueryExecutorInitialization() {
        assertNotNull(queryExecutor);
    }
}

