package smartrics.iotics.samples.http;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;
import org.apache.logging.log4j.util.Strings;
import org.eclipse.rdf4j.common.lang.FileFormat;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static smartrics.iotics.samples.http.ContentTypesMap.mimeFor;

public class SparqlEndpoint extends AbstractVerticle {
    private static final Logger LOGGER = LoggerFactory.getLogger(SparqlEndpoint.class);
    private final Database database;
    private final String port;
    private final String securePort;

    public SparqlEndpoint(String httpPort, String httpSecurePort, Database database) {
        this.database = database;
        this.port = httpPort;
        this.securePort = httpSecurePort;
    }

    private static String generateShortUUID() {
        UUID uuid = UUID.randomUUID();
        ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[16]);
        byteBuffer.putLong(uuid.getMostSignificantBits());
        byteBuffer.putLong(uuid.getLeastSignificantBits());
        return Base64.getUrlEncoder().withoutPadding().encodeToString(byteBuffer.array());
    }

    private static void getValidQuery(HttpServerRequest request) {
        String query = request.getParam("query");
        if ("get".equalsIgnoreCase(request.method().name())) {
            if (query == null) {
                // service description
                return;
            } else if (query.isEmpty()) {
                throw new ValidationException(400, ErrorMessage.toJson("missing query"));
            }
        }

        if ("post".equalsIgnoreCase(request.method().name())) {
            // Clients must set the content type header of the HTTP request to application/sparql-query
            // spec par 2.1.3
            String ct = request.getHeader("Content-Type");
            if (Strings.isBlank(ct)) {
                throw new ValidationException(400, ErrorMessage.toJson("missing content type"));
            }
            String mime = ct.split(";")[0].trim();
            if (!"application/sparql-query".equals(mime) && !"application/x-www-form-urlencoded".equals(mime)) {
                throw new ValidationException(400, ErrorMessage.toJson("invalid content type"));
            }
        }
    }

    private static void validateGraphName(RoutingContext ctx) {
        // SPARQL-1.1 Par 2.1.4
        List<String> def = ctx.queryParam("default-graph-uri");
        List<String> named = ctx.queryParam("named-graph-uri");
        if (!def.isEmpty() || !named.isEmpty()) {
            throw new ValidationException(400, ErrorMessage.toJson("RDF datasets not allowed"));
        }
    }

    private static @NotNull FileFormat getValidAcceptedResultType(HttpServerRequest request) {
        String accepted = request.getHeader("Accept");
        if (accepted != null) {
            // TODO: better support multiple Accept with quality flag
            accepted = accepted.split(",")[0].trim();
            accepted = accepted.split(";")[0];
        }

        FileFormat mappedAccepted = ContentTypesMap.UNRECOGNISED;
        if (accepted != null && !accepted.equals("*/*")) {
            mappedAccepted = ContentTypesMap.get(accepted, ContentTypesMap.UNRECOGNISED);
        }

        if (mappedAccepted.equals(ContentTypesMap.UNRECOGNISED)) {
            throw new ValidationException(400, ErrorMessage.toJson("Unsupported response mime type: " + accepted));
        }
        return mappedAccepted;
    }

    public Router createRouter(Database database) {
        Router router = Router.router(vertx);

        router.route().handler(this::logRequestAndResponse);

        // Handle /health route separately
        router.get("/*").handler(StaticHandler.create("webroot"));
        router.get("/health").handler(this::handleHealth);

        // Apply the BodyHandler and validateRequest handler to the /sparql routes
        router.route("/sparql*").handler(BodyHandler.create()).handler(this::validateRequest);

        // Define the /sparql routes
        router.get("/sparql").handler(ctx -> this.handleGet(ctx, database));
        router.post("/sparql").handler(ctx -> this.handlePost(ctx, database));
        return router;
    }

    private void logRequestAndResponse(RoutingContext ctx) {
        String remoteAddress = ctx.request().remoteAddress().toString();
        String uri = ctx.request().uri();
        String absoluteUri = ctx.request().absoluteURI();
        String method = ctx.request().method().toString();
        String headers = ctx.request().headers().entries().stream().map(e -> e.getKey() + ":" + e.getValue()).collect(Collectors.joining(", "));
        String id = generateShortUUID();

        LOGGER.info("Request [id={}][URI={}][method={}][from={}][absoluteURI={}][headers={}]", id, uri, method, remoteAddress, absoluteUri, headers);
        ctx.addBodyEndHandler(v -> {
            int statusCode = ctx.response().getStatusCode();
            String statusMessage = ctx.response().getStatusMessage();
            String respHeaders = ctx.response().headers().entries().stream().map(e -> e.getKey() + ":" + e.getValue()).collect(Collectors.joining(", "));
            LOGGER.info("Response [id={}][statusCode={}][statusMessage={}][headers={}]", id, statusCode, statusMessage, respHeaders);
        });
        ctx.next();
    }

    public void start() {
        Router router = createRouter(database);
        LOGGER.info("Starting on port {}", port);
        vertx.createHttpServer()
                .requestHandler(router)
                .listen(Integer.parseInt(port), http -> {
                    if (http.succeeded()) {
                        LOGGER.info("HTTP server started on port {}", port);
                    } else {
                        LOGGER.error("HTTP server failed to start", http.cause());
                    }
                });

        // HTTPS server
        HttpServerOptions options = new HttpServerOptions()
                .setSsl(true)
                .setKeyCertOptions(new PemKeyCertOptions()
                        .setCertPath("./ssl/server.crt")
                        .setKeyPath("./ssl/server.key")
                );

        LOGGER.info("Starting on secure port {}", securePort);
        vertx.createHttpServer(options)
                .requestHandler(router)
                .listen(Integer.parseInt(securePort), http -> {
                    if (http.succeeded()) {
                        LOGGER.info("HTTPS server started on port {}", securePort);
                    } else {
                        LOGGER.error("HTTPS server failed to start", http.cause());
                    }
                });
    }

    private void handleHealth(RoutingContext ctx) {
        ctx.response().setStatusCode(200);
        ctx.response().end("{ \"status\" : \"OK\" }");
    }

    private void handleGet(RoutingContext ctx, Database database) {
        try {
            String encodedQuery = ctx.request().getParam("query");
            if (encodedQuery == null) {
                // service description
                ctx.response().setStatusCode(200);
                ctx.response().send(serviceDescription());
            } else {
                String query = URLDecoder.decode(encodedQuery, StandardCharsets.UTF_8);
                handle(ctx, database, query);
            }
        } catch (ValidationException e) {
            sendError(e.getCode(), e.getMessage(), ctx.response());
        }
    }

    private String serviceDescription() {
        String endpoint = "/sparql";
        return """
                {
                  "@context": {
                    "sd": "http://www.w3.org/ns/sparql-service-description#",
                    "void": "http://www.w3.org/ns/void#"
                  },
                  "@type": "sd:Service",
                  "sd:endpoint": { "@id": "http://localhost/@@@@" },
                  "void:sparqlEndpoint": { "@id": "http://localhost/@@@@" },
                  "sd:name": "IOTICS SPARQL Endpoint",
                  "sd:description": "This is a SPARQL endpoint for accessing IOTICSpace via HTTP",
                  "sd:supportsQueryLanguage": { "@id": "sd:SPARQL11Query" },
                  "sd:resultFormat": [
                    { "sd:contentType": "application/sparql-results+xml" },
                    { "sd:contentType": "application/sparql-results+json" },
                    { "sd:contentType": "text/csv" },
                    { "sd:contentType": "application/rdf+xml" },
                    { "sd:contentType": "text/turtle" },
                    { "sd:contentType": "application/x-turtle" },
                    { "sd:contentType": "application/n-triples" }
                  ],
                  "void:triplestore": "IOTICSpace"
                }
                """.replaceAll("@@@@", endpoint);
    }

    private void handlePost(RoutingContext ctx, Database database) {
        try {
            String query = ctx.body().asString();
            String ct = ctx.request().getHeader("Content-Type");
            ct = ct.split(";")[0].trim();
            if ("application/x-www-form-urlencoded".equals(ct)) {
                // TODO: we should check the charset in the content type if available
                query = URLDecoder.decode(query, StandardCharsets.UTF_8);
            }
            if (query != null) {
                handle(ctx, database, query);
            } else {
                ctx.response().setStatusCode(200);
                ctx.response().send();
            }
        } catch (ValidationException e) {
            sendError(e.getCode(), e.getMessage(), ctx.response());
        }
    }

    private void handle(RoutingContext ctx, Database database, String query) {
        try {
            FileFormat type = ctx.get("acceptedResponseType");
            String mime = mimeFor(type);
            if (mime != null) {
                ctx.response().headers().set("Content-Type", mime);
            }
            ctx.response().headers().add("Access-Control-Allow-Origin", "*");
            database.run(query, ctx, mime);
        } catch (Exception e) {
            LOGGER.warn("exception when handling request", e);
            String message = e.getMessage();
            if (e.getCause() != null) {
                message = message + ": " + e.getCause().getMessage();
            }
            sendError(500, ErrorMessage.toJson(message), ctx.response());
        }
    }

    private void sendError(int statusCode, String message, HttpServerResponse response) {
        response.setStatusCode(statusCode).setStatusMessage(message).end();
    }

    private void validateRequest(RoutingContext ctx) throws ValidationException {
        try {
            HttpServerRequest request = ctx.request();
            validateGraphName(ctx);
            getValidQuery(request);
            FileFormat mappedAccepted = getValidAcceptedResultType(request);
            ctx.put("acceptedResponseType", mappedAccepted);

            ctx.next();
        } catch (ValidationException e) {
            sendError(e.getCode(), e.getMessage(), ctx.response());
        }
    }

    public static class ValidationException extends RuntimeException {
        private final int code;

        public ValidationException(int code, String message) {
            super(message);
            this.code = code;
        }

        public int getCode() {
            return code;
        }
    }

}

