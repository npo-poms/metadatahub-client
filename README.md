# MetadataHub SPARQL Client

A pure Java client library for securely accessing the MetadataHub SPARQL endpoint with OAuth2 authentication via Keycloak. **No Spring dependencies** - just pure Java with standard libraries.

## Features

- ✅ **Pure Java**: No Spring Framework or other heavyweight frameworks
- ✅ **OAuth2 Authentication**: Automatic token acquisition and refresh using Keycloak client credentials flow
- ✅ **Token Caching**: Intelligent token caching with automatic refresh before expiration
- ✅ **SPARQL Query Support**: Execute SELECT, CONSTRUCT, and ASK queries
- ✅ **Modern Java**: Uses `java.net.http.HttpClient` (Java 11+)
- ✅ **Records**: Modern Java records for configuration
- ✅ **Result Parsing**: Apache Jena-based SPARQL result parsing
- ✅ **Reusable**: Works in any Java environment (Spring, plain Java, Gradle, Maven, etc.)

## Documentation References

- [MetadataHub Authentication](https://docs.metadatahub.bijnpo.nl/handleiding/developers/authentication/)
- [MetadataHub SPARQL Service](https://docs.metadatahub.bijnpo.nl/handleiding/developers/sparql-service/)

## Installation

### Prerequisites

- Java 17+
- Maven 3.6+
- Access to MetadataHub Keycloak server and SPARQL endpoint

### Add Dependency

Add to your `pom.xml`:

```xml
<dependency>
    <groupId>nl.npo.metadatahub</groupId>
    <artifactId>client</artifactId>
    <version>0.1-SNAPSHOT</version>
</dependency>
```

Or build locally:

```bash
mvn clean install
```

## Quick Start

### Basic Usage (Standalone)

```java
import nl.npo.metadatahub.client.auth.OAuth2Config;
import nl.npo.metadatahub.client.auth.TokenManager;
import nl.npo.metadatahub.client.sparql.SparqlConfig;
import nl.npo.metadatahub.client.sparql.SparqlHttpClient;
import nl.npo.metadatahub.client.sparql.SparqlQueryExecutor;
import nl.npo.metadatahub.client.sparql.model.SparqlResult;

// Configure OAuth2
OAuth2Config oauthConfig = new OAuth2Config(
    "https://auth.metadatahub.bijnpo.nl/realms/metadatahub/protocol/openid-connect/token",
    "your-client-id",
    "your-client-secret",
    "openid profile email"
);

// Create token manager
TokenManager tokenManager = new TokenManager(oauthConfig);

// Configure SPARQL endpoint
SparqlConfig sparqlConfig = new SparqlConfig(
    "https://sparql.metadatahub.bijnpo.nl/sparql"
);

// Create SPARQL client
SparqlHttpClient httpClient = new SparqlHttpClient(tokenManager, sparqlConfig);

// Create executor
SparqlQueryExecutor executor = new SparqlQueryExecutor(httpClient);

// Execute query
SparqlResult result = executor.select("SELECT ?s ?p ?o WHERE { ?s ?p ?o } LIMIT 10");
```

## Usage Examples

### Execute SELECT Query

```java
String query = """
    PREFIX schema: <http://schema.org/>
    SELECT ?resource ?title
    WHERE {
        ?resource a schema:Thing ;
                 schema:name ?title .
    }
    LIMIT 100
    """;

try {
    SparqlResult result = executor.select(query);
    System.out.println("Variables: " + result.getVariables());
    result.getResults().forEach(row -> 
        System.out.println(row.get("resource") + " = " + row.get("title"))
    );
} catch (SparqlHttpClient.SparqlException e) {
    System.err.println("Query failed: " + e.getMessage());
}
```

### Execute CONSTRUCT Query

```java
String query = """
    PREFIX schema: <http://schema.org/>
    CONSTRUCT {
        ?resource schema:name ?title .
    }
    WHERE {
        ?resource a schema:Thing ;
                 schema:name ?title .
    }
    """;

SparqlResult result = executor.construct(query);
// Result contains RDF data
```

### Execute ASK Query

```java
String query = """
    PREFIX schema: <http://schema.org/>
    ASK {
        ?resource a schema:Thing .
    }
    """;

boolean hasResults = executor.ask(query);
System.out.println("Data exists: " + hasResults);
```

## Configuration

### Using Default Timeouts

```java
// Default: 10s connect, 30s read
SparqlConfig config = new SparqlConfig("https://sparql.example.com/sparql");
```

### Custom Timeouts

```java
import java.time.Duration;

SparqlConfig config = new SparqlConfig(
    "https://sparql.example.com/sparql",
    Duration.ofSeconds(5),
    Duration.ofSeconds(60)
);
```

### OAuth2 with Default Scope

```java
OAuth2Config config = new OAuth2Config(
    "https://auth.example.com/token",
    "client-id",
    "client-secret"
    // scope defaults to "openid profile email"
);
```

### Custom OAuth2 Scope

```java
OAuth2Config config = new OAuth2Config(
    "https://auth.example.com/token",
    "client-id",
    "client-secret",
    "read write"
);
```

## Advanced Usage

### Custom HttpClient

```java
import java.net.http.HttpClient;

// Use your own HttpClient with custom settings
HttpClient customClient = HttpClient.newBuilder()
    .version(HttpClient.Version.HTTP_2)
    .build();

SparqlHttpClient httpClient = new SparqlHttpClient(customClient, tokenManager, sparqlConfig);
```

### Error Handling

```java
import nl.npo.metadatahub.client.sparql.SparqlHttpClient.SparqlException;
import nl.npo.metadatahub.client.auth.TokenManager.TokenException;

try {
    SparqlResult result = executor.select(query);
} catch (SparqlException e) {
    System.err.println("SPARQL error: " + e.getMessage());
    e.printStackTrace();
} catch (TokenException e) {
    System.err.println("Authentication error: " + e.getMessage());
}
```

### Token  Management

```java
// Get current token
String token = tokenManager.getAccessToken();

// Manually invalidate token (forces new token request on next call)
tokenManager.invalidateToken();
```

## Architecture

### Records (Modern Java Configuration)

Configuration objects use Java records for immutability:

```java
// OAuth2Config: tokenUri, clientId, clientSecret, scope
// SparqlConfig: endpoint, connectTimeout, readTimeout
```

### Components

1. **TokenManager**: Handles OAuth2 token acquisition and caching
2. **SparqlHttpClient**: Low-level HTTP client for SPARQL requests
3. **SparqlQueryExecutor**: High-level API for queries
4. **OAuth2Config**: Record for OAuth2 settings
5. **SparqlConfig**: Record for SPARQL endpoint settings

### Flow Diagram

```
Application
    ↓
SparqlQueryExecutor
    ↓
SparqlHttpClient (with Bearer token)
    ↓
TokenManager ← getAccessToken()
    ↓
Keycloak Server (OAuth2)
    ↓
SPARQL Endpoint
```

## Error Handling

### SparqlException

Thrown when SPARQL queries fail. Check the message and cause for details.

### TokenException

Thrown when OAuth2 token acquisition fails. Indicates authentication or network issues.

### Automatic Token Refresh

If a 401 (Unauthorized) response is received, the client automatically:
1. Invalidates the cached token
2. Requests a new token from Keycloak
3. Retries the original query

## Testing

### Unit Tests

Run with:

```bash
mvn test
```

### Integration Testing

To test with a real Keycloak and SPARQL endpoint:

```java
@Test
void testRealEndpoint() throws Exception {
    OAuth2Config oauthConfig = new OAuth2Config(...);
    TokenManager tokenManager = new TokenManager(oauthConfig);
    SparqlConfig sparqlConfig = new SparqlConfig(...);
    SparqlHttpClient httpClient = new SparqlHttpClient(tokenManager, sparqlConfig);
    SparqlQueryExecutor executor = new SparqlQueryExecutor(httpClient);
    
    SparqlResult result = executor.select("SELECT * WHERE { ?s ?p ?o } LIMIT 1");
    assertFalse(result.getResults().isEmpty());
}
```

## Authentication Flow (Client Credentials)

This client uses OAuth2 Client Credentials flow:

1. Client sends client ID and secret to Keycloak token endpoint
2. Keycloak validates and returns an access token
3. Client caches token and uses it for all SPARQL requests
4. Tokens are automatically refreshed before expiration
5. If token is rejected (401), it's refreshed and request is retried

## Dependencies

### Core
- **java.net.http**: Java 11+ built-in HTTP client
- **Apache Jena**: SPARQL query result parsing
- **Jackson**: JSON parsing for OAuth2 responses
- **SLF4J**: Logging (optional backend implementation required at runtime)

### Testing
- **JUnit 5**: Test framework
- **Mockito**: Mocking
- **SLF4J Simple**: Simple logging for tests

## Why Pure Java?

Modern Java has excellent HTTP support built-in (`java.net.http.HttpClient`). This library avoids heavyweight frameworks like Spring Boot while maintaining:
- ✅ Full OAuth2 support
- ✅ Automatic token management
- ✅ SPARQL result parsing
- ✅ Clean, intuitive API
- ✅ Reusable in any Java context (Spring, standalone, etc.)

## Building

```bash
# Compile
mvn clean compile

# Run tests
mvn test

# Package
mvn package

# Install locally
mvn install
```

## Example Usage

See `src/main/java/nl/npo/metadatahub/client/SparqlClientExample.java` for complete working examples.

## Troubleshooting

### 401 Unauthorized

- Verify `client-id` and `client-secret` are correct
- Check that the Keycloak server is accessible
- Ensure the token endpoint URL is correct
- Verify the OAuth2 scope requirements

### 400 Bad Request

- Verify the SPARQL query syntax is valid
- Check that all SPARQL endpoints are reachable
- Ensure proper URL encoding of queries

### Connection Timeouts

- Increase timeout values in `SparqlConfig`
- Check network connectivity to both Keycloak and SPARQL endpoint
- Verify firewall rules

### Token Caching Issues

- Call `tokenManager.invalidateToken()` to force a fresh token
- Check token expiration time
- Verify token scope includes required permissions

## Development

### Project Structure

```
src/main/java/nl/npo/metadatahub/client/
├── auth/
│   ├── OAuth2Config.java (record)
│   └── TokenManager.java
├── sparql/
│   ├── SparqlConfig.java (record)
│   ├── SparqlHttpClient.java
│   ├── SparqlQueryExecutor.java
│   ├── model/
│   │   └── SparqlResult.java
│   └── SparqlException
└── SparqlClientExample.java
```

### Code Style

- Pure Java with no framework dependencies
- Records for immutable configuration
- SLF4J for logging
- Exception-based error handling

## License

NPO POMS - See LICENSE file for details

## Support

For issues or questions:
- Refer to [MetadataHub Documentation](https://docs.metadatahub.bijnpo.nl/)
- Check the GitHub repository: https://github.com/npo-poms/metadatahub-client
- Review examples in `SparqlClientExample.java`

