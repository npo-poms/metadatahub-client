package nl.npo.metadatahub.client;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * MetadataHub SPARQL Client Spring Boot Application.
 *
 * This application provides a client library for interacting with the MetadataHub SPARQL endpoint
 * with OAuth2 authentication via Keycloak.
 */
@SpringBootApplication
@ComponentScan(basePackages = "nl.npo.metadatahub.client")
public class MetadataHubClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(MetadataHubClientApplication.class, args);
    }
}

