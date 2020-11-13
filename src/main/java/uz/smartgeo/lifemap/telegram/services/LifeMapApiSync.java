package uz.smartgeo.lifemap.telegram.services;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClientOptions;
import org.apache.http.HttpResponse;
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

public class LifeMapApiSync {
    protected CloseableHttpClient httpClient;
    private static Logger LOGGER = Logger.getLogger(LifeMapApiSync.class);

    protected int httpPort;
    protected String httpHost;
    protected String lifeMapApiUrl;

    public LifeMapApiSync(String host, Integer port) {
        httpPort = port;
        httpHost = host;
        lifeMapApiUrl = host + ":" + port;

        httpClient = HttpClientBuilder.create().build();
    }

    public JsonObject getUserByExtIdSync(int extUserId) {
        try {
//            CloseableHttpClient httpClient = HttpClientBuilder.create().build();
            HttpResponse response;
            JSONObject json = new JSONObject();

            JsonObject userJson = null;
            JsonObject userRes;

            HttpPost post = new HttpPost(lifeMapApiUrl + "/api/user/getByExtId");

            json.put("extId", extUserId);
            json.put("authType", 1);

            StringEntity se = new StringEntity(json.toString());
            se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
            post.setEntity(se);
            response = httpClient.execute(post);

            /*Checking response */
            if (response != null) {
                InputStream in = response.getEntity().getContent(); //Get the data in the entity

                userRes = Converters.getJsonObject(in);

                if (userRes != null && userRes.getJsonObject("result") != null) {
                    userJson = userRes.getJsonObject("result");
                    return userJson;
                }
            }
           } catch (Exception e) {
            e.printStackTrace();
            LOGGER.error(e.getMessage());
        }
        return null;
    }

    public boolean updateUserMobileNumberSync(int userId, String mobileNumber) {
        try {
//            CloseableHttpClient httpClient = HttpClientBuilder.create().build();
            HttpResponse response;
            JSONObject json = new JSONObject();

            JsonObject updateRes = null;
            Integer updatedUserId;

            HttpPost post = new HttpPost(lifeMapApiUrl + "/api/user/updateMobileNumber");

            //Переопределение строки удаляем + для записи в базу
            mobileNumber = mobileNumber.replace("+", "");

            json.put("extId", userId);
            json.put("authType", 1);
            json.put("mobileNumber", mobileNumber);

            StringEntity se = new StringEntity(json.toString());
            se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
            post.setEntity(se);
            response = httpClient.execute(post);

            /*Checking response */
            if (response != null) {
                InputStream in = response.getEntity().getContent(); //Get the data in the entity

                updateRes = Converters.getJsonObject(in);

                if (updateRes != null && updateRes.getJsonObject("result") != null) {

                    if (updateRes.getJsonObject("result").getInteger("id") != null) {
                        updatedUserId = updateRes.getJsonObject("result").getInteger("id");
                    } else {
                        return false;
                    }

                    return (updatedUserId != null && updatedUserId > 0);
                } else {

                }
            } else {
                return false;
            }
          } catch (Exception e) {
            e.printStackTrace();
            LOGGER.error(e.getMessage());
        }
        return false;
    }

    public boolean updateModerationStatusSync(long eventId, boolean isModerated) {
        try {
//            CloseableHttpClient httpClient = HttpClientBuilder.create().build();
            HttpResponse response;
            JSONObject json = new JSONObject();

            JsonObject updateRes;
            Integer updatedUserId;

            HttpPost post = new HttpPost(lifeMapApiUrl + "/api/event/updateModerationStatus");

            json.put("isModerated", isModerated);
            json.put("eventId", eventId);

            StringEntity se = new StringEntity(json.toString());
            se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
            post.setEntity(se);
            response = httpClient.execute(post);

            /*Checking response */
            if (response != null) {
                InputStream in = response.getEntity().getContent(); //Get the data in the entity

                updateRes = Converters.getJsonObject(in);

                if (updateRes != null && updateRes.getJsonObject("result") != null) {

                    if (updateRes.getJsonObject("result").getInteger("id") != null) {
                        updatedUserId = updateRes.getJsonObject("result").getInteger("id");
                    } else {
                        return false;
                    }
                    return (updatedUserId != null && updatedUserId > 0);
                } else {

                }
            } else {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.error(e.getMessage());
        }
        return false;
    }

}
