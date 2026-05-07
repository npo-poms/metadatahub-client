package nl.npo.metadatahub.client.auth;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import nl.npo.metadatahub.client.sparql.*;

public class Configuration {

    private final Properties properties;

    public Configuration(Properties properties) {
        this.properties = properties;
    }

    public Configuration(String propsFile) throws IOException {
        Path configdir = Path.of(System.getProperty("user.home")).resolve("conf");
        Path configFile = configdir.resolve(propsFile);
        Properties props = new Properties();
        props.load(Files.newInputStream(configFile));
        this(props);
    }

    public Configuration() throws IOException {
        this("metadatahub.properties");
    }

    public TokenManager getTokenManager() {
        return new TokenManager(OAuth2Config.fromProperties(properties));
    }

    public SparqlConfig getSparqlConfig() {
        return new SparqlConfig(properties.getProperty("sparql.endpoint"));
    }

    public SparqlQueryExecutor getSparqlQueryExecutor() {
        TokenManager tokenManager = getTokenManager();
        SparqlConfig sparqlConfig = getSparqlConfig();
        SparqlHttpClient httpClient = new SparqlHttpClient(tokenManager, sparqlConfig);
        return new SparqlQueryExecutor(httpClient);
    }



}
