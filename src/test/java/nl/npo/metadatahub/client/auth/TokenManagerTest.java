package nl.npo.metadatahub.client.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.RestTemplate;
import nl.npo.metadatahub.client.auth.TokenManager.TokenException;
import nl.npo.metadatahub.client.config.MetadataHubProperties;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TokenManager.
 */
@ExtendWith(MockitoExtension.class)
class TokenManagerTest {

    @Mock
    private RestTemplate mockRestTemplate;

    @Mock
    private RestTemplateBuilder mockBuilder;

    private MetadataHubProperties properties;
    private TokenManager tokenManager;

    @BeforeEach
    void setUp() {
        properties = new MetadataHubProperties();
        properties.getOauth2().setTokenUri("http://auth-server/token");
        properties.getOauth2().setClientId("test-client");
        properties.getOauth2().setClientSecret("test-secret");

        when(mockBuilder.build()).thenReturn(mockRestTemplate);
        tokenManager = new TokenManager(mockBuilder, properties);
    }

    @Test
    void testGetAccessTokenSuccess() throws TokenException {
        Map<String, Object> tokenResponse = new HashMap<>();
        tokenResponse.put("access_token", "test-token-123");
        tokenResponse.put("expires_in", 3600);

        when(mockRestTemplate.postForObject(
            anyString(),
            any(),
            eq(Map.class)
        )).thenReturn(tokenResponse);

        String token = tokenManager.getAccessToken();

        assertEquals("test-token-123", token);
        verify(mockRestTemplate, times(1)).postForObject(anyString(), any(), eq(Map.class));
    }

    @Test
    void testGetAccessTokenCached() throws TokenException {
        Map<String, Object> tokenResponse = new HashMap<>();
        tokenResponse.put("access_token", "test-token-456");
        tokenResponse.put("expires_in", 3600);

        when(mockRestTemplate.postForObject(
            anyString(),
            any(),
            eq(Map.class)
        )).thenReturn(tokenResponse);

        String token1 = tokenManager.getAccessToken();
        String token2 = tokenManager.getAccessToken();

        assertEquals(token1, token2);
        verify(mockRestTemplate, times(1)).postForObject(anyString(), any(), eq(Map.class));
    }

    @Test
    void testGetAccessTokenMissingToken() {
        Map<String, Object> tokenResponse = new HashMap<>();
        tokenResponse.put("expires_in", 3600);

        when(mockRestTemplate.postForObject(
            anyString(),
            any(),
            eq(Map.class)
        )).thenReturn(tokenResponse);

        assertThrows(TokenException.class, () -> tokenManager.getAccessToken());
    }

    @Test
    void testInvalidateToken() throws TokenException {
        Map<String, Object> tokenResponse = new HashMap<>();
        tokenResponse.put("access_token", "test-token-789");
        tokenResponse.put("expires_in", 3600);

        when(mockRestTemplate.postForObject(
            anyString(),
            any(),
            eq(Map.class)
        )).thenReturn(tokenResponse);

        String token1 = tokenManager.getAccessToken();
        tokenManager.invalidateToken();
        String token2 = tokenManager.getAccessToken();

        assertEquals(token1, token2);
        verify(mockRestTemplate, times(2)).postForObject(anyString(), any(), eq(Map.class));
    }
}

