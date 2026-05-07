package nl.npo.metadatahub.poms;

import java.util.*;
import java.util.function.Consumer;
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
        set("title", item, builder::mainTitle);
        set("description", item, builder::mainDescription);
        set("prid", item, builder::mid);
        return builder.build();

    }

    protected void set(String field, QuerySolution item, Consumer<String> consumer) {
        if (fields.contains(field)) {
            consumer.accept(item.getLiteral(field).getString());
        }
    }
}
