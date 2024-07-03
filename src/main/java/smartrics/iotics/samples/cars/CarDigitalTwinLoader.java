package smartrics.iotics.samples.cars;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.protobuf.ByteString;
import com.iotics.api.*;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import smartrics.iotics.host.Builders;
import smartrics.iotics.host.IoticsApi;
import smartrics.iotics.identity.SimpleIdentityManager;
import smartrics.iotics.samples.http.Database;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
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

    public CarDigitalTwinLoader(EventBus eventBus, Database database, IoticsApi api, SimpleIdentityManager sim, int sharePeriodSec) {
        this.executorService = Executors.newSingleThreadExecutor();
        this.handlersExecutor = Executors.newCachedThreadPool();
        this.scheduler = new RandomScheduler<>(sharePeriodSec, sharePeriodSec / 2, 8);
        this.gson = new GsonBuilder()
                .registerTypeAdapter(CarDigitalTwin.class, new CarDigitalTwinDeserializer(api, sim, sharePeriodSec))
                .registerTypeAdapter(GeoLocation.class, new GeoLocationDeserializer()).create();
        this.eventBus = eventBus;
        this.eventBus.register(this);
        this.database = database;
        this.api = api;
        this.sim = sim;
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

        Consumer<Void> onSuccess = unused -> LOGGER.info("Successfully scheduled sharing [did={}]", car.getMyIdentity().did());
        Consumer<Throwable> onError = throwable -> LOGGER.info("Exception sharing [did={}]", car.getMyIdentity().did(), throwable);

        retrieveValueIDs(car.getMyIdentity().did(), bindings -> scheduler.start(() -> {
            OperationalStatus opStatus = car.getOpStatus();
            List<Binding> status = Binding.filter(bindings, "status");
            database.set(Sem.createStatusModel(car.getMyIdentity().did(), status, opStatus));

            LocationData locationData = car.getLocationData();
            List<Binding> loc = Binding.filter(bindings, "locationData");
            database.set(Sem.createLocationDataModel(car.getMyIdentity().did(), loc, locationData));

            CompletableFuture<Void> future = car.share();
            future.thenAccept(unused -> LOGGER.info("Shared Car data [did={}]", car.getMyIdentity().did()));
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
                LOGGER.info("Processed car {}: {}, with did: {}", counter.incrementAndGet(), car.getLabel(), done.getPayload().getTwinId().getId());
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

    private void retrieveValueIDs(String did, Consumer<List<Binding>> handler) {
        String didQ = "<" + did + ">";
        String sparql = """
                PREFIX iotics: <http://data.iotics.com/iotics#>
                SELECT ?pointID ?pointName ?valueID ?valueKey
                WHERE {
                    # Bind the resource ID
                    BIND (""" + didQ + """ 
                    AS ?resource)

                    # Get the advertise IDs
                    ?resource iotics:advertises ?pointID .

                    # For each advertise ID, get the point name and presented values
                    ?pointID iotics:pointName ?pointName ;
                                 iotics:presents ?valueID .

                    # For each presented value, get the value key
                    OPTIONAL {
                        ?valueID iotics:valueKey ?valueKey .
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
                List<Binding> binds = new ArrayList<>();
                try {
                    JsonObject jsonObject = JsonParser.parseString(bet).getAsJsonObject();
                    JsonArray bindings = jsonObject.getAsJsonObject("results")
                            .getAsJsonArray("bindings");
                    bindings.forEach(jsonElement -> {
                        JsonObject el = jsonElement.getAsJsonObject();
                        String pointID = el.getAsJsonObject("pointID").get("value").getAsString();
                        String pointName = el.getAsJsonObject("pointName").get("value").getAsString();
                        String valueID = el.getAsJsonObject("valueID").get("value").getAsString();
                        String valueKey = el.getAsJsonObject("valueKey").get("value").getAsString();
                        binds.add(new Binding(pointID, pointName, valueID, valueKey));
                    });
                } catch (Exception e) {
                    LOGGER.warn("unable to get the ID", e);
                }
                handler.accept(binds);
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
