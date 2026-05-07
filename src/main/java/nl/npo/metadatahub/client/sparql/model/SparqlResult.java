package nl.npo.metadatahub.client.sparql.model;

import java.util.List;
import java.util.Map;

/**
 * Represents a SPARQL query result set.
 */
public class SparqlResult {

    private List<String> variables;
    private List<Map<String, String>> results;

    public SparqlResult(List<String> variables, List<Map<String, String>> results) {
        this.variables = variables;
        this.results = results;
    }

    public List<String> getVariables() {
        return variables;
    }

    public List<Map<String, String>> getResults() {
        return results;
    }

    @Override
    public String toString() {
        return "SparqlResult{" +
                "variables=" + variables +
                ", resultCount=" + results.size() +
                '}';
    }
}

