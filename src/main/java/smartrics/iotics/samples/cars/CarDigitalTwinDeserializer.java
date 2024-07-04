package smartrics.iotics.samples.cars;

import com.google.gson.*;
import com.iotics.api.GeoLocation;
import smartrics.iotics.host.IoticsApi;
import smartrics.iotics.identity.Identity;
import smartrics.iotics.identity.IdentityManager;

import java.lang.reflect.Type;

public class CarDigitalTwinDeserializer implements JsonDeserializer<CarDigitalTwin> {

    private final IoticsApi api;
    private final IdentityManager sim;

    public CarDigitalTwinDeserializer(IoticsApi api, IdentityManager sim) {
        this.api = api;
        this.sim = sim;
    }

    @Override
    public CarDigitalTwin deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();

        Integer unit = jsonObject.get("unit").getAsInt();
        String comment = jsonObject.get("comment").getAsString();
        String label = jsonObject.get("label").getAsString();
        String manufacturerName = jsonObject.get("manufacturerName").getAsString();
        String colour = jsonObject.get("colour").getAsString();
        String model = jsonObject.get("model").getAsString();
        String identifier = jsonObject.get("identifier").getAsString();
        String owner = jsonObject.get("owner").getAsString();

        JsonObject locationObj = jsonObject.get("location").getAsJsonObject();
        double latitude = locationObj.get("latitude").getAsDouble();
        double longitude = locationObj.get("longitude").getAsDouble();
        GeoLocation location = GeoLocation.newBuilder().setLat(latitude).setLon(longitude).build();

        Identity myId = sim.newTwinIdentityWithControlDelegation(identifier);

        return CarDigitalTwin.CarDigitalTwinBuilder.aCarDigitalTwin()
                .withApi(api)
                .withSim(sim)
                .withMyIdentity(myId)
                .withUnit(unit)
                .withComment(comment)
                .withLabel(label)
                .withOwner(owner)
                .withManufacturerName(manufacturerName)
                .withColour(colour)
                .withModel(model)
                .withIdentifier(identifier)
                .withOwner(owner)
                .withLocation(location)
                .build();
    }
}
