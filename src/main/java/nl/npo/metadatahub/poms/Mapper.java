package nl.npo.metadatahub.poms;

import java.util.*;
import nl.vpro.domain.media.MediaBuilder;
import nl.vpro.domain.media.Program;
import org.apache.jena.query.QuerySolution;

public class Mapper {

    private final Set<String> fields;

    public Mapper(Collection<String> fields) {
        this.fields = new HashSet<>(fields);
    }


    public Program toProgram(QuerySolution item) {
        var builder = MediaBuilder.program();
        if (fields.contains("title")) {
            builder.mainTitle(item.getLiteral("title").getString());
        }
        if (fields.contains("description")) {
            builder.mainDescription(item.getLiteral("description").getString());
        }
        if (fields.contains("prid")) {
            builder.mid(item.getLiteral("prid").getString());
        }
        return builder.build();

    }
}
