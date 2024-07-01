package smartrics.iotics.samples.cars;

import org.greenrobot.eventbus.EventBus;
import smartrics.iotics.host.HostEndpoints;
import smartrics.iotics.host.HttpServiceRegistry;
import smartrics.iotics.host.IoticsApi;
import smartrics.iotics.host.IoticsApiImpl;
import smartrics.iotics.host.grpc.HostConnection;
import smartrics.iotics.host.grpc.HostConnectionImpl;
import smartrics.iotics.identity.SimpleConfig;
import smartrics.iotics.identity.SimpleIdentityImpl;
import smartrics.iotics.identity.SimpleIdentityManager;
import smartrics.iotics.identity.jna.JnaSdkApiInitialiser;
import smartrics.iotics.identity.jna.OsLibraryPathResolver;
import smartrics.iotics.identity.jna.SdkApi;

import java.io.*;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class Main {

    public static void main(String[] args) throws IOException {
        HttpServiceRegistry sr = new HttpServiceRegistry("demo.dev.iotics.space");
        HostEndpoints endpoints = sr.find();
        SimpleIdentityManager sim = newSimpleIdentityManager(endpoints.resolver());
        IoticsApi ioticsApi = newIoticsApi(sim, endpoints.grpc(), Duration.of(1, TimeUnit.HOURS.toChronoUnit()));

        EventBus eventBus = EventBus.getDefault();
        CarDigitalTwinLoader loader = new CarDigitalTwinLoader(eventBus, ioticsApi, sim);

        File initialFile = new File("src/main/resources/car_data_100.json");

        if(initialFile.canRead()) {
            try (InputStream inputStream = new FileInputStream(initialFile)) {
                loader.loadCarData(inputStream);
            } finally {
                loader.shutdown();
            }
        } else {
            throw new IllegalArgumentException("unable to access " + initialFile.getAbsolutePath());
        }

    }

    public static IoticsApi newIoticsApi(SimpleIdentityManager sim, String grpcEndpoint, Duration tokenDuration) {
        HostConnection connection = new HostConnectionImpl(grpcEndpoint, sim, tokenDuration);
        return new IoticsApiImpl(connection);
    }


    public static SimpleIdentityManager newSimpleIdentityManager(String resolver) throws FileNotFoundException {
        SimpleConfig userConf = SimpleConfig.readConf(Path.of("src/main/resources/identity/user.json"));
        SimpleConfig agentConf = SimpleConfig.readConf(Path.of("src/main/resources/identity/demoAgent.json"));
        OsLibraryPathResolver pathResolver = new OsLibraryPathResolver() {};
        SdkApi api = new JnaSdkApiInitialiser("./lib", pathResolver).get();
        return SimpleIdentityManager.Builder.anIdentityManager()
                .withSimpleIdentity(new SimpleIdentityImpl(api, resolver, userConf.seed(), agentConf.seed()))
                .withAgentKeyID(agentConf.keyId())
                .withUserKeyID(userConf.keyId())
                .withAgentKeyName(agentConf.keyName())
                .withUserKeyName(userConf.keyName())
                .build();
    }

}
