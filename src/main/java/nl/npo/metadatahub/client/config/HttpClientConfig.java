package nl.npo.metadatahub.client.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Configuration for HTTP client and related beans.
 */
@Configuration
public class HttpClientConfig {

    /**
     * Provide a RestClient bean configured with timeouts.
     * RestClient is the modern replacement for deprecated RestTemplate.
     */
    @Bean
    public RestClient restClient(MetadataHubProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(properties.getClient().getConnectTimeoutMs()))
            .build();
        
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofMillis(properties.getClient().getReadTimeoutMs()));
        
        return RestClient.builder()
            .requestFactory(factory)
            .build();
    }
}

