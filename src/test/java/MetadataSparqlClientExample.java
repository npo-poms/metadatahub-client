import jakarta.xml.bind.JAXB;
import java.util.List;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.logging.Logger;
import nl.npo.metadatahub.client.Configuration;
import nl.npo.metadatahub.client.auth.*;
import nl.npo.metadatahub.client.sparql.MetadataSparqlClient;
import nl.npo.metadatahub.poms.Mapper;
import nl.npo.metadatahub.poms.MetadataToPoms;
import org.apache.jena.query.QuerySolution;
import nl.vpro.domain.media.Program;
import org.apache.jena.query.ResultSet;


static final Logger log = Logger.getLogger("main");

/**
 * Example: How to use the MetadataHub SPARQL client.
 * This shows how to use the client standalone, without Spring.
 */

void main() throws Exception {
    System.setProperty("log4j2.root.level","INFO");

    var configuration = new Configuration();
    var client = configuration.createClient();
    var poms = new MetadataToPoms(client);

    IO.println(poms.getProgram(client, "mid:12345"));
}

private static void firstQuery(MetadataSparqlClient client) throws Exception {
    String firstQuery = """
    PREFIX ec: <http://www.ebu.ch/metadata/ontologies/ebucoreplus#>
    PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
    SELECT ?prid
           (SAMPLE(?dateCreated) AS ?dateCreated)
           (SAMPLE(?dateModified) AS ?dateModified)
           (SAMPLE(?title) AS ?title)
           (SAMPLE(?description) AS ?description)
           (SAMPLE(?channelName) AS ?channelName)
           (SAMPLE(?start) AS ?start)
           (SAMPLE(?end) AS ?end)
           (GROUP_CONCAT(DISTINCT ?genreLabel; separator="||") AS ?genreLabels)
           (GROUP_CONCAT(DISTINCT ?targetAudienceLabel; separator="||") AS ?targetAudienceLabels)
           (GROUP_CONCAT(DISTINCT CONCAT(STR(?ratingType), "::", STR(?ratingValue)); separator="||") AS ?ratings)
    WHERE {
      ?entity ec:hasIdentifier ?id .
      ?id ec:name "PRID" .
      ?id ec:identifierValue ?prid .
      ?entity ec:hasPublication ?publication .
        ?publication ec:hasPublicationChannel ?channel .
        ?channel ec:name ?channelName .
        ?publication ec:hasStartDateTime ?start .
      OPTIONAL { ?publication ec:hasEndDateTime ?end . }
      OPTIONAL { ?entity ec:title ?title . }
      OPTIONAL { ?entity ec:contentDescription ?description . }
      OPTIONAL { ?entity ec:hasDateCreated ?dateCreated . }
      OPTIONAL { ?entity ec:hasDateModified ?dateModified . }
      OPTIONAL {
          ?entity ec:hasGenre ?genre .
          ?genre skos:prefLabel ?genreLabel .
        }
        OPTIONAL {
          ?entity ec:hasTargetAudience ?audience .
          ?audience ec:hasObjectType/skos:prefLabel ?targetAudienceLabel .
        }
        OPTIONAL {
          ?entity ec:hasRating ?rating .
          ?rating a ?ratingType .
          ?rating ec:ratingValue ?ratingValue .
        }
    }
    GROUP BY ?prid
    LIMIT 10""";

    ResultSet result = client.selectQuery(firstQuery);
    List<String> fields = result.getResultVars();
    Mapper mapper = new Mapper(fields);

    while(result.hasNext()) {
        var row = result.next();
        Program program = mapper.toProgram(row);
        JAXB.marshal(program, System.out);

        List<String> genres = splitAggregatedValues(row, "genreLabels");
        List<String> targetAudiences = splitAggregatedValues(row, "targetAudienceLabels");
        List<String> ratings = splitAggregatedValues(row, "ratings");
        log.info("prid=" + row.get("prid") + " genres=" + genres + " targetAudiences=" + targetAudiences + " ratings=" + ratings);
    }
}

private static List<String> splitAggregatedValues(QuerySolution row, String field) {
    if (!row.contains(field) || row.getLiteral(field) == null) {
        return List.of();
    }
    String value = row.getLiteral(field).getString();
    if (value.isBlank()) {
        return List.of();
    }
    return Arrays.stream(value.split("\\|\\|"))
        .filter(v -> !v.isBlank())
        .collect(Collectors.toList());
}


