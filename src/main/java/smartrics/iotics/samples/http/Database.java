package smartrics.iotics.samples.http;

import io.vertx.core.http.HttpServerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.vertx.ext.web.RoutingContext;
import org.eclipse.rdf4j.common.lang.FileFormat;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.query.*;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultWriter;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultWriterFactory;
import org.eclipse.rdf4j.query.resultio.sparqljson.SPARQLResultsJSONWriterFactory;
import org.eclipse.rdf4j.query.resultio.sparqlxml.SPARQLResultsXMLWriterFactory;
import org.eclipse.rdf4j.query.resultio.text.csv.SPARQLResultsCSVWriterFactory;
import org.eclipse.rdf4j.query.resultio.text.tsv.SPARQLResultsTSVWriterFactory;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static smartrics.iotics.samples.http.ContentTypesMap.UNRECOGNISED;

public class Database {
    private static final Logger LOGGER = LoggerFactory.getLogger(Database.class);

    private final Repository repository;

    public Database() {
        repository = new SailRepository(new MemoryStore());
        repository.init();
    }

    public void set(Model model) {
        try (RepositoryConnection conn = repository.getConnection()) {
            model.subjects().forEach(resource ->
            {
                try {
                    conn.remove(resource, null, null);
                } catch (Exception e) {
                    LOGGER.warn("unable to remove model for resource " + resource.stringValue());
                }
            });
            try {
                conn.add(model);
                LOGGER.warn("added " + model);
            } catch (Exception e) {
                LOGGER.warn("unable to remove model for resource " + model);
            }
        }
    }

    public void run(String query, RoutingContext context, String acceptHeader) {
        try (RepositoryConnection conn = repository.getConnection()) {
            Query preparedQuery = conn.prepareQuery(QueryLanguage.SPARQL, query);

            switch (preparedQuery) {
                case TupleQuery tupleQuery -> handleTupleQuery(tupleQuery, context, acceptHeader);
                case GraphQuery graphQuery -> handleGraphQuery(graphQuery, context, acceptHeader);
                case BooleanQuery booleanQuery -> handleBooleanQuery(booleanQuery, context);
                case null, default -> context.response().setStatusCode(400).end("Unsupported query type");
            }
        } catch (Exception e) {
            context.response().setStatusCode(500).end(e.getMessage());
        }
    }

    private void handleTupleQuery(TupleQuery tupleQuery, RoutingContext context, String acceptHeader) {
        HttpServerResponse response = context.response();
        response.setChunked(true); // Enable chunked transfer
        response.putHeader("Content-Type", acceptHeader);

        try (TupleQueryResult result = tupleQuery.evaluate()) {
            Optional<TupleQueryResultWriterFactory> writerFactoryOpt = getTupleQueryResultWriterFactory(acceptHeader);

            if (writerFactoryOpt.isEmpty()) {
                context.fail(500, new RuntimeException("Unsupported MIME type: " + acceptHeader));
                return;
            }

            TupleQueryResultWriterFactory writerFactory = writerFactoryOpt.get();
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            TupleQueryResultWriter tupleWriter = writerFactory.getWriter(byteArrayOutputStream);
            tupleWriter.startQueryResult(result.getBindingNames());
            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                tupleWriter.handleSolution(bindingSet);
            }
            tupleWriter.endQueryResult();
            response.write(byteArrayOutputStream.toString(StandardCharsets.UTF_8));

            // End the response
            response.end();
        } catch (Exception e) {
            context.fail(500, e);
        }
    }

    private void handleGraphQuery(GraphQuery graphQuery, RoutingContext context, String acceptHeader) {
        try (GraphQueryResult result = graphQuery.evaluate()) {
            Model model = QueryResults.asModel(result);
            StringWriter out = new StringWriter();
            FileFormat format = ContentTypesMap.get(acceptHeader, UNRECOGNISED);
            if (format.equals(UNRECOGNISED)) {
                context.response().setStatusCode(400).end(ErrorMessage.toJson("unrecognised or invalid accept header"));
            } else {
                Rio.write(model, out, (RDFFormat) format);
                context.response()
                        .putHeader("content-type", format.getDefaultMIMEType())
                        .end(out.toString());
            }
        } catch (Exception e) {
            context.response().setStatusCode(500).end(ErrorMessage.toJson(e.getMessage()));
        }
    }

    private void handleBooleanQuery(BooleanQuery booleanQuery, RoutingContext context) {
        try {
            boolean result = booleanQuery.evaluate();
            context.response()
                    .putHeader("content-type", "application/sparql-results+json")
                    .end("{\"head\": {}, \"boolean\": " + result + "}");
        } catch (Exception e) {
            context.response().setStatusCode(500).end(e.getMessage());
        }
    }


    private Optional<TupleQueryResultWriterFactory> getTupleQueryResultWriterFactory(String acceptHeader) {
        if ("text/csv".equals(acceptHeader)) {
            return Optional.of(new SPARQLResultsCSVWriterFactory());
        } else if ("application/sparql-results+xml".equals(acceptHeader)) {
            return Optional.of(new SPARQLResultsXMLWriterFactory());
        } else if ("application/sparql-results+json".equals(acceptHeader)) {
            return Optional.of(new SPARQLResultsJSONWriterFactory());
        } else if ("text/tab-separated-values".equals(acceptHeader)) {
            return Optional.of(new SPARQLResultsTSVWriterFactory());
        }
        return Optional.empty();
    }

}
