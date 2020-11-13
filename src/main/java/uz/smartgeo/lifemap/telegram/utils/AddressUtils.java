package uz.smartgeo.lifemap.telegram.utils;

import io.vertx.core.json.JsonObject;

public class AddressUtils {

    public static JsonObject getAddressFormatted(JsonObject addressRes) {
        JsonObject addressGeoJson = new JsonObject();

        String addressDisplayName = addressRes.getString("display_name");

        String countryCode = "";
        String region = "";
        String addressLongitude = addressRes.getString("lon");
        String addressLatitude = addressRes.getString("lat");

        addressGeoJson.put("display_name", addressDisplayName);
        addressGeoJson.put("country_code", countryCode);
        addressGeoJson.put("region", region);
        addressGeoJson.put("lat", addressLatitude);
        addressGeoJson.put("lon", addressLongitude);

        return addressGeoJson;
    }

    public static String getAddressCountryCode(JsonObject address) {
        String countryCode = "";

        if (address.getString("country_code") != null) {
            countryCode = address.getString("country_code");
        }

        return countryCode;
    }

    public static String getAddressRegion(JsonObject address) {
        String region = "";

        if (address.getString("state") != null) {
            region = address.getString("state");
        } else if (address.getString("city") != null) {
            region = address.getString("city");
        } else if (address.getString("county") != null) {
            region = address.getString("county");
        }

        return region;
    }


}
