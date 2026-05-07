package nl.npo.metadatahub.client.sparql;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import nl.npo.metadatahub.client.sparql.SparqlHttpClient.SparqlException;
import nl.npo.metadatahub.client.sparql.model.SparqlResult;

/**
 * Main SPARQL query executor service.
 * Pure Java implementation, no Spring dependencies.
 */
public class SparqlQueryExecutor {
    
    private static final Logger logger = LoggerFactory.getLogger(SparqlQueryExecutor.class);
    
    private final SparqlHttpClient httpClient;

    /**
     * Create executor with HTTP client.
     *
     * @param httpClient SPARQL HTTP client
     */
    public SparqlQueryExecutor(SparqlHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /**
     * Execute a SELECT query.
     *
     * @param sparqlQuery the SPARQL SELECT query
     * @return result set containing query results
     * @throws SparqlException if the query fails
     */
    public SparqlResult select(String sparqlQuery) throws SparqlException {
        logger.info("Executing SPARQL SELECT query");
        return httpClient.selectQuery(sparqlQuery);
    }

    /**
     * Execute a CONSTRUCT query.
     *
     * @param sparqlQuery the SPARQL CONSTRUCT query
     * @return result set containing RDF data
     * @throws SparqlException if the query fails
     */
    public SparqlResult construct(String sparqlQuery) throws SparqlException {
        logger.info("Executing SPARQL CONSTRUCT query");
        return httpClient.constructQuery(sparqlQuery);
    }

    /**
     * Execute an ASK query.
     *
     * @param sparqlQuery the SPARQL ASK query
     * @return true if the query has results, false otherwise
     * @throws SparqlException if the query fails
     */
    public boolean ask(String sparqlQuery) throws SparqlException {
        logger.info("Executing SPARQL ASK query");
        return httpClient.askQuery(sparqlQuery);
    }
}

