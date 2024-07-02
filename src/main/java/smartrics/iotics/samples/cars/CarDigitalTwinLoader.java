package smartrics.iotics.samples.cars;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.protobuf.ByteString;
import com.iotics.api.*;
import io.grpc.stub.StreamObserver;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import smartrics.iotics.host.Builders;
import smartrics.iotics.host.IoticsApi;
import smartrics.iotics.identity.SimpleIdentityManager;
import smartrics.iotics.samples.http.Database;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class CarDigitalTwinLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(CarDigitalTwinLoader.class);

    private final ExecutorService executorService;
    private final Gson gson;
    private final EventBus eventBus;
    private final Executor handlersExecutor;

    private final AtomicInteger counter = new AtomicInteger(0);
    private final Database database;
    private final IoticsApi api;
    private final SimpleIdentityManager sim;
    private final RandomScheduler<Void> scheduler;
    private final int sharePeriodSec;

    public CarDigitalTwinLoader(EventBus eventBus, Database database, IoticsApi api, SimpleIdentityManager sim, int sharePeriodSec) {
        this.executorService = Executors.newSingleThreadExecutor();
        this.handlersExecutor = Executors.newCachedThreadPool();
        this.scheduler = new RandomScheduler<>(sharePeriodSec, sharePeriodSec / 2, 8);
        this.gson = new GsonBuilder()
                .registerTypeAdapter(CarDigitalTwin.class, new CarDigitalTwinDeserializer(api, sim))
                .registerTypeAdapter(GeoLocation.class, new GeoLocationDeserializer()).create();
        this.eventBus = eventBus;
        this.eventBus.register(this);
        this.database = database;
        this.api = api;
        this.sim = sim;
        this.sharePeriodSec = sharePeriodSec;
    }

    public void shutdown() {
        executorService.shutdown();
    }

    public void loadCarData(boolean shareOnly, InputStream inputStream) throws IOException {
        String jsonData;
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
            jsonData = bufferedReader.lines().collect(Collectors.joining("\n"));
        }
        executorService.submit(() -> {
            try (JsonReader reader = new JsonReader(new StringReader(jsonData))) {
                reader.beginArray();
                while (reader.hasNext()) {
                    CarDigitalTwin car = gson.fromJson(reader, CarDigitalTwin.class);
                    if (shareOnly) {
                        eventBus.post(new CarShareEvent(car));
                    } else {
                        eventBus.post(new CarCreateEvent(car));
                    }
                }
                reader.endArray();
            } catch (IOException e) {
                throw new IllegalStateException("unable to load data", e);
            } finally {
                shutdown();
            }
        });
    }

    @Subscribe
    public void startShare(CarShareEvent event) {
        CarDigitalTwin car = event.car();

        Consumer<Void> onSuccess = unused -> LOGGER.debug("Successfully scheduled sharing [did=" + car.getMyIdentity().did() + "]");
        Consumer<Throwable> onError = throwable -> LOGGER.debug("Exception sharing [did=" + car.getMyIdentity().did() + "]", throwable);

        retrieveValueID(car.getMyIdentity().did(), valueID -> scheduler.start (() -> {
            OperationalStatus opStatus = car.getOpStatus();
            Boolean object = opStatus.opStatus();
            ModelBuilder builder = new ModelBuilder();
            builder.setNamespace("ns", "https://ontologies.metaphacts.com/iotics-car-digital-twin/")
                    .subject("ns:" + valueID)
                    .add("ns:payloadValue", object);
            database.set(builder.build());
            CompletableFuture<Void> future = car.share();
            future.thenAccept(unused -> LOGGER.info("Shared Car data [did=" + car.getMyIdentity().did() + ", payload=" + object + "]"));
            return null;
        }, onSuccess, onError));
    }

    @Subscribe
    public void createCar(CarCreateEvent event) {
        CarDigitalTwin car = event.car();
        ListenableFuture<UpsertTwinResponse> result = car.upsert();
        result.addListener(() -> {
            try {
                UpsertTwinResponse done = result.get();
                LOGGER.debug("Processed car " + counter.incrementAndGet() + ": " + car.getLabel() + ", with did: " + done.getPayload().getTwinId().getId());
                eventBus.post(new CarShareEvent(event.car()));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                // retry
                LOGGER.debug("Exception creating car", result.exceptionNow());
                eventBus.post(new CarCreateEvent(car));
            }

        }, handlersExecutor);
    }

    private void retrieveValueID(String did, Consumer<String> handler) {
        String didQ = "<" + did + ">";
        String sparql = """
                PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
                PREFIX iot: <http://data.iotics.com/iotics#>

                SELECT ?presentID
                WHERE {
                  ?resource iot:advertises ?advertisedID .
                  ?advertisedID iot:presents ?presentID .
                  VALUES ?resource {
                """ + didQ + """
                  }
                }
                """;
        api.metaAPI().sparqlQuery(SparqlQueryRequest.newBuilder()
                .setHeaders(Builders.newHeadersBuilder(sim.agentIdentity()).build())
                .setScope(Scope.LOCAL)
                .setPayload(SparqlQueryRequest.Payload.newBuilder()
                        .setQuery(ByteString.copyFrom(sparql.getBytes(StandardCharsets.UTF_8)))
                        .build())
                .build(), new StreamObserver<>() {
            @Override
            public void onNext(SparqlQueryResponse sparqlQueryResponse) {
                String bet = sparqlQueryResponse.getPayload().getResultChunk().toStringUtf8();
                String id = null;
                try {
                    JsonObject jsonObject = JsonParser.parseString(bet).getAsJsonObject();
                    JsonArray bindings = jsonObject.getAsJsonObject("results")
                            .getAsJsonArray("bindings");
                    if (!bindings.isEmpty()) {
                        JsonObject firstBinding = bindings.get(0).getAsJsonObject();
                        JsonObject presentID = firstBinding.getAsJsonObject("presentID");
                        id = presentID.get("value").getAsString();
                    }
                } catch (Exception e) {
                    LOGGER.warn("unable to get the ID", e);
                }
                handler.accept(id);
            }

            @Override
            public void onError(Throwable throwable) {
                LOGGER.warn("Unable to exec sparql for getting the value ID", throwable);
            }

            @Override
            public void onCompleted() {

            }
        });
    }

}
