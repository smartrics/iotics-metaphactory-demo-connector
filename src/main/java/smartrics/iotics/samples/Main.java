package smartrics.iotics.samples;

import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
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
import smartrics.iotics.samples.cars.CarDigitalTwinLoader;
import smartrics.iotics.samples.http.Database;
import smartrics.iotics.samples.http.SparqlEndpoint;

import java.io.*;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class Main {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    private final static String ENV_SHARE_ONLY = "SHARE_ONLY";
    private final static String ENV_CARS_FILE = "CAR_FILE";
    private final static String ENV_HOST_DNS = "HOST_DNS";
    private final static String ENV_TOKEN_DURATION_SECONDS = "TOKEN_DURATION_SECONDS";
    private final static String ENV_SHARE_PERIOD_SECONDS = "SHARE_PERIOD_SECONDS";
    private final static String ENV_USER_CONF_JSON = "USER_CONF_JSON";
    private final static String ENV_AGENT_CONF_JSON = "AGENT_CONF_JSON";
    private final static String ENV_SPARQL_PORT = "SPARQL_PORT";
    private final static String ENV_SPARQL_SECURE_PORT = "SPARQL_SECURE_PORT";

    public static void main(String[] args) throws IOException {
        boolean shareOnly = Boolean.parseBoolean(Optional.ofNullable(System.getenv(ENV_SHARE_ONLY)).orElse("false"));
        String carsFile = Optional.ofNullable(System.getenv(ENV_CARS_FILE)).orElse("src/main/resources/car_data.json");
        String hostDns = Optional.ofNullable(System.getenv(ENV_HOST_DNS)).orElse("demo.dev.iotics.space");
        int tokenDuration = Integer.parseInt(Optional.ofNullable(System.getenv(ENV_TOKEN_DURATION_SECONDS)).orElse("3600"));
        int sharePeriodSec = Integer.parseInt(Optional.ofNullable(System.getenv(ENV_SHARE_PERIOD_SECONDS)).orElse("3"));
        String userIdPath = Optional.ofNullable(System.getenv(ENV_USER_CONF_JSON)).orElse("src/main/resources/identity/user.json");
        String agentIdPath = Optional.ofNullable(System.getenv(ENV_AGENT_CONF_JSON)).orElse("src/main/resources/identity/demoAgent.json");
        String httpPort = Optional.ofNullable(System.getenv(ENV_SPARQL_PORT)).orElse("8080");
        String httpsPort = Optional.ofNullable(System.getenv(ENV_SPARQL_SECURE_PORT)).orElse("8443");


        EventBus eventBus = EventBus.getDefault();
        Database database = new Database();

        // Create an instance of Vertx
        Vertx vertx = Vertx.vertx();

        // Create an instance of your Verticle
        Verticle sparqlEndpointVerticle = new SparqlEndpoint(httpPort, httpsPort, database);

        // Deploy the Verticle
        vertx.deployVerticle(sparqlEndpointVerticle, res -> {
            if (res.succeeded()) {
                LOGGER.info("Deployment id is: " + res.result());
            } else {
                LOGGER.info("Deployment failed!");
            }
        });

        HttpServiceRegistry sr = new HttpServiceRegistry(hostDns);
        HostEndpoints endpoints = sr.find();
        SimpleIdentityManager sim = newSimpleIdentityManager(userIdPath, agentIdPath, endpoints.resolver());
        IoticsApi ioticsApi = newIoticsApi(sim, endpoints.grpc(), Duration.of(tokenDuration, TimeUnit.SECONDS.toChronoUnit()));

        CarDigitalTwinLoader loader = new CarDigitalTwinLoader(eventBus, database, ioticsApi, sim, sharePeriodSec);

        File initialFile = new File(carsFile);

        if (initialFile.canRead()) {
            try (InputStream inputStream = new FileInputStream(initialFile)) {
                loader.loadCarData(shareOnly, inputStream);
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


    public static SimpleIdentityManager newSimpleIdentityManager(String userIdPath, String agentIdPath, String resolver) throws FileNotFoundException {
        SimpleConfig userConf = SimpleConfig.readConf(Path.of(userIdPath));
        SimpleConfig agentConf = SimpleConfig.readConf(Path.of(agentIdPath));
        OsLibraryPathResolver pathResolver = new OsLibraryPathResolver() {
        };
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
