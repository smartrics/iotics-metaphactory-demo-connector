package smartrics.iotics.samples.cars;

import com.google.gson.*;
import com.iotics.api.GeoLocation;

import java.lang.reflect.Type;

class GeoLocationDeserializer implements JsonDeserializer<GeoLocation> {
    @Override
    public GeoLocation deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        double latitude = jsonObject.get("latitude").getAsDouble();
        double longitude = jsonObject.get("longitude").getAsDouble();
        return GeoLocation.newBuilder().setLat(latitude).setLon(longitude).build();
    }
}
