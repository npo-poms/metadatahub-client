package nl.npo.metadatahub.client.sparql.model;

import java.util.List;
import java.util.Map;

/**
 * Represents a SPARQL query result set.
 */
public record SparqlResult(List<String> variables, List<Map<String, String>> results) {

    @Override
    public String toString() {
        return "SparqlResult{" +
            "variables=" + variables +
            ", resultCount=" + results.size() +
            '}';
    }
}

