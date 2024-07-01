package smartrics.iotics.samples.cars;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.iotics.api.UpsertTwinResponse;
import org.apache.logging.log4j.core.util.ExecutorServices;
import org.checkerframework.checker.units.qual.A;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import com.iotics.api.GeoLocation;
import smartrics.iotics.host.IoticsApi;
import smartrics.iotics.identity.SimpleIdentityManager;

import java.io.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class CarDigitalTwinLoader {
    private final ExecutorService executorService;
    private final Gson gson;
    private final EventBus eventBus;
    private final Executor handlersExecutor;

    private final AtomicInteger counter = new AtomicInteger(0);

    public CarDigitalTwinLoader(EventBus eventBus, IoticsApi api, SimpleIdentityManager sim) {
        this.executorService = Executors.newSingleThreadExecutor();
        this.handlersExecutor = Executors.newCachedThreadPool();
        this.gson = new GsonBuilder()
                .registerTypeAdapter(CarDigitalTwin.class, new CarDigitalTwinDeserializer(api, sim))
                .registerTypeAdapter(GeoLocation.class, new GeoLocationDeserializer()).create();
        this.eventBus = eventBus;
        this.eventBus.register(this);
    }

    public void shutdown() {
        executorService.shutdown();
    }

    public void loadCarData(InputStream inputStream) throws IOException {
        String jsonData;
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
            jsonData = bufferedReader.lines().collect(Collectors.joining("\n"));
        }
        executorService.submit(() -> {
            try (JsonReader reader = new JsonReader(new StringReader(jsonData))) {
                reader.beginArray();
                while (reader.hasNext()) {
                    CarDigitalTwin car = gson.fromJson(reader, CarDigitalTwin.class);
                    eventBus.post(new CarEvent(car));
                }
                reader.endArray();
            } catch (IOException e) {
                throw new IllegalStateException("unable to load data", e);
            }
            finally {
                shutdown();
            }
        });
    }

    @Subscribe
    public void handleCarEvent(CarEvent event) {
        CarDigitalTwin car = event.car();
        // Process the car data
        // Return false if the processing fails
//        System.out.println("Processing car: " + car.getLabel());
        ListenableFuture<UpsertTwinResponse> result = car.upsert();

        result.addListener(() -> {
            try {
                UpsertTwinResponse done = result.get();
                System.out.println("Processed car " + counter.incrementAndGet() + ": " + car.getLabel() + ", with did: " + done.getPayload().getTwinId().getId());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                // retry
                result.exceptionNow().printStackTrace();
                eventBus.post(new CarEvent(car));
            }

        }, handlersExecutor);
    }
}
