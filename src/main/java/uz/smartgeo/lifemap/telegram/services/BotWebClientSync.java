package uz.smartgeo.lifemap.telegram.services;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClientOptions;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import uz.smartgeo.lifemap.telegram.utils.Converters;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class BotWebClientSync {

    private static Logger LOGGER = Logger.getLogger(BotWebClientSync.class);

    protected CloseableHttpClient httpClient;

    protected String nominatimUrl;

    protected String host;
    protected Integer port;

    protected String telegramBotUrl;

    public BotWebClientSync(JsonObject config) {
        WebClientOptions options = new WebClientOptions()
                .setUserAgent("bot-web-client/1.0.3");

        options
                .setKeepAlive(false);

        nominatimUrl = config.getString("nominatim.url");

        host = config.getString("server.host");
        port = config.getInteger("server.port");

        if (port != null) {
            telegramBotUrl = "http://" + host + ":" + port;
        } else {
            telegramBotUrl = "https://" + host;
        }

        httpClient = HttpClientBuilder.create().build();
    }

    public JsonObject getAddressByCoordinates(Double lon, Double lat, String lang) {
        try {
//            CloseableHttpClient httpClient = HttpClientBuilder.create().build();
            HttpResponse response;
            JSONObject json = new JSONObject();

            JsonObject addressJson = null;
            JsonObject addressRes;

            HttpGet getRequest = new HttpGet(nominatimUrl + "?format=json&zoom=18&lat=" + lat + "&lon=" + lon + "&accept-language=" + lang);

            response = httpClient.execute(getRequest);

            if (response != null) {
                InputStream in = response.getEntity().getContent(); //Get the data in the entity

                addressRes = Converters.getJsonObject(in);

                if (addressRes != null) {

                    return addressRes;
                }
            } else {
                System.out.println(response);
                LOGGER.error("Error in getAddressByCoordinates");
            }
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.error(e.getMessage());
        }
        return null;
    }

    public JsonObject notifyAboutNewEvent(Integer telegramUserId, Long dbUserId, Long eventId,
                                          int categoryId,
                                          String eventName,
                                          String eventDescription,
                                          String address,
                                          String startDate,
                                          String endDate,
                                          String regDate,
                                          String extUserName,
                                          String extUserLastName,
                                          String extUserLogin,
                                          Long eventRegionId
                                          ) {
        try {
//            CloseableHttpClient httpClient = HttpClientBuilder.create().build();
            HttpResponse response;
            JSONObject json = new JSONObject();

            JsonObject notifyRes;

            HttpPost post = new HttpPost(telegramBotUrl + "/notify/eventAdded");

            json.put("user_id", telegramUserId);
            json.put("ext_user_id", dbUserId);
            json.put("id", eventId);

            json.put("category_id", categoryId);
            json.put("name", eventName);
            json.put("description", eventDescription);
            json.put("address", address);

            json.put("start_date", startDate);
            json.put("end_date", endDate);
            json.put("reg_date", regDate);

            json.put("ext_user_name", extUserName);
            json.put("moderation_status", 1);
            json.put("ext_user_last_name", extUserLastName);
            json.put("ext_user_login", extUserLogin);
            json.put("region_id", eventRegionId);

            StringEntity se = new StringEntity(json.toString(), StandardCharsets.UTF_8);
            se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
            post.setEntity(se);

            response = httpClient.execute(post);

            if (response != null) {
                InputStream in = response.getEntity().getContent(); //Get the data in the entity

                notifyRes = Converters.getJsonObject(in);

                if (notifyRes != null) {
                    return notifyRes;
                }
            } else {
                System.out.println(response);
                LOGGER.error("Error when notify telegram bot about new event");
            }
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.error(e.getMessage());
        }
        return null;
    }

}
