package uz.smartgeo.lifemap.telegram.services;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.codec.BodyCodec;
import org.apache.log4j.Logger;

public class LifeMapApi {
    protected WebClient webClient;
    private static Logger LOGGER = Logger.getLogger(LifeMapApi.class);

    protected int httpPort;
    protected String httpHost;

    public LifeMapApi(Vertx vertx, String host, Integer port) {
        WebClientOptions options = new WebClientOptions()
                .setUserAgent("LM-bot/1.0.1");

        options
                .setKeepAlive(false)
                .setDefaultHost(host)
                .setDefaultPort(port);

        httpPort = port;
        httpHost = host;
        webClient = WebClient.create(vertx, options);
    }

    public Future<JsonObject> getUserByExtId(long extId) {
        LOGGER.info("---getUserByExtId---");
        Promise<JsonObject> promise = Promise.promise();

        JsonObject postParams = new JsonObject()
                .put("extId", extId)
                .put("authType", 1);

        webClient
                .post("/api/user/getByExtId")
                .as(BodyCodec.jsonObject())
                .sendJsonObject(postParams, ar -> {
                    if (ar.succeeded()) {
                        LOGGER.info("DATA RES");

                        LOGGER.info(ar.result().body());

                        promise.complete(ar.result().body());
                    } else {
                        LOGGER.info(ar);
                        promise.fail("DATA FAILED");
                        System.out.println("Something went wrong " + ar.cause().getMessage());
                        LOGGER.error("Error in getUserByExtId. " + ar.cause().toString());
                    }
                });

        return promise.future();
    }

    public Future<JsonArray> getUsersByRoleId(long roleId) {
        LOGGER.info("---getUserById---");
        Promise<JsonArray> promise = Promise.promise();

        JsonObject postParams = new JsonObject()
                .put("roleId", roleId);

        webClient
                .post("/api/user/getUsersByRoleId")
                .as(BodyCodec.jsonObject())
                .sendJsonObject(postParams, ar -> {
                    if (ar.succeeded()) {
                        LOGGER.info("DATA RES");
//                        LOGGER.info(ar.result().body());

                        JsonObject resultMap = JsonObject.mapFrom(ar.result().body().getMap());
                        JsonArray users = resultMap.getJsonArray("result");

                        promise.complete(users);
                    } else {
                        LOGGER.info(ar);
                        promise.fail("DATA FAILED");
                        System.out.println("Something went wrong " + ar.cause().getMessage());
                        LOGGER.error("Error in getUserByRoleId. " + ar.cause().toString());
                    }
                });

        return promise.future();
    }


    public void updateUserMobileNumber(int userId, String mobileNumber) {
        //  public Future<Long> updateUserMobileNumber(int userId, String mobileNumber) {

        LOGGER.info("---updateUserMobilePhone---");
        Promise<Long> promise = Promise.promise();

        //Переопределение строки удаляем + для записи в базу
        mobileNumber = mobileNumber.replace("+", "");

        JsonObject postParams = new JsonObject()
                .put("extId", userId)
                .put("authType", 1)
                .put("mobileNumber", mobileNumber);

        LOGGER.info("userId: " + userId);
        LOGGER.info("mobileNumber: " + mobileNumber);

        webClient
                .post("/api/user/updateMobileNumber")
                .as(BodyCodec.jsonObject())

                .sendJsonObject(postParams, ar -> {
                    if (ar.succeeded()) {
                        LOGGER.info("UPDATE DONE");
                        LOGGER.info(ar.result());

                        JsonObject result = JsonObject.mapFrom(ar.result().body().getMap().get("result"));

                        if (result.getLong("id") != null && result.getLong("id") > 0) {
                            promise.complete(result.getLong("id"));
                        } else {
                            promise.fail("updateUserMobileNumber FAILED");
                        }
                    } else {
                        LOGGER.info(ar);
                        promise.fail("DATA FAILED");
                        System.out.println("Something went wrong " + ar.cause().getMessage());
                        LOGGER.error("Error in updateUserMobileNumber. " + ar.cause().toString());
                    }
                });

    }

}
