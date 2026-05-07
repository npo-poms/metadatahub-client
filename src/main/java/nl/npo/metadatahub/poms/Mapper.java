package nl.npo.metadatahub.poms;

import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import nl.vpro.domain.media.MediaBuilder;
import nl.vpro.domain.media.Program;
import org.apache.jena.datatypes.xsd.XSDDateTime;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.Literal;

public class Mapper {

    private final Set<String> fields;

    public Mapper(Collection<String> fields) {
        this.fields = new HashSet<>(fields);
    }

    public Program toProgram(QuerySolution item) {
        var builder = MediaBuilder.program();
        setString("title", item, builder::mainTitle);
        setString("description", item, builder::mainDescription);
        setString("prid", item, builder::mid);
        setInstant("dateCreated", item, builder::creationInstant);
        setInstant("dateModified", item, builder::lastModified);
        return builder.build();
    }

    protected void setString(String field, QuerySolution item, Consumer<String> consumer) {
        set(field, item, consumer, Literal::getString);
    }

    protected void setInstant(String field, QuerySolution item, Consumer<Instant> consumer) {
        set(field, item, consumer, lit -> ((XSDDateTime) lit.getValue()).asCalendar().toInstant());
    }


    protected <T> void set(String field, QuerySolution item, Consumer<T> consumer, Function<Literal, T> converter) {
        if (fields.contains(field)) {
            var lit = item.getLiteral(field);
            if (lit == null) {
                consumer.accept(null);
            } else {
                consumer.accept(converter.apply(lit));
            }
        }
    }
}
