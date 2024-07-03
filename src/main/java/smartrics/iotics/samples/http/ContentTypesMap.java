package smartrics.iotics.samples.http;

import com.iotics.api.SparqlResultType;
import org.eclipse.rdf4j.common.lang.FileFormat;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultFormat;
import org.eclipse.rdf4j.rio.RDFFormat;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static com.iotics.api.SparqlResultType.*;

public class ContentTypesMap {

    private static final Map<String, FileFormat> resultFormat = new ConcurrentHashMap<>(8);

    private static final List<SparqlResultType> SPARQL_RESULT_TYPES = Arrays.asList(SPARQL_XML, SPARQL_JSON, SPARQL_CSV);
    private static final List<SparqlResultType> RDF_RESULT_TYPES = Arrays.asList(RDF_XML, RDF_TURTLE, RDF_NTRIPLES);

    public static final FileFormat UNRECOGNISED = new FileFormat("", "", Charset.defaultCharset(), "");

    static {
        resultFormat.put("application/sparql-results+xml", TupleQueryResultFormat.SPARQL);
        resultFormat.put("text/xml", TupleQueryResultFormat.SPARQL);
        resultFormat.put("application/sparql-results+json", TupleQueryResultFormat.JSON);
        resultFormat.put("text/json", TupleQueryResultFormat.JSON);
        resultFormat.put("text/csv", TupleQueryResultFormat.CSV);
        resultFormat.put("text/tab-separated-values", TupleQueryResultFormat.TSV);

        resultFormat.put("application/rdf+xml", RDFFormat.RDFXML);
        resultFormat.put("text/turtle", RDFFormat.TURTLE);
        resultFormat.put("application/x-turtle", RDFFormat.TURTLE); // Assuming the same enum value for Turtle
        resultFormat.put("application/n-triples", RDFFormat.NTRIPLES);
        resultFormat.put("application/n-quads", RDFFormat.NQUADS);
        resultFormat.put("application/ld+json", RDFFormat.JSONLD);
        resultFormat.put("application/rdf+json", RDFFormat.RDFJSON);
        resultFormat.put("application/x-binary-rdf", RDFFormat.BINARY);

    }

    public static String mimeFor(FileFormat e) {
        Optional<String> res = resultFormat.entrySet().stream()
                .filter(entry -> entry.getValue().equals(e)).map(Map.Entry::getKey).findFirst();
        return res.orElse(null);
    }

    public static FileFormat get(String v, FileFormat def) {
        return get(v).orElse(def);
    }

    public static Optional<FileFormat> get(String v) {
        if (v == null) {
            return Optional.empty();
        }
        FileFormat ff = resultFormat.getOrDefault(v, UNRECOGNISED);
        return Optional.of(ff);
    }

}
