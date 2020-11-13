package uz.smartgeo.lifemap.telegram.services;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class ConfigService {

    //private static Logger LOGGER = Logger.getLogger(ConfigService.class);

    public static Future<JsonObject> getConfig(Vertx vertx, String filename) {
        Promise<JsonObject> promise = Promise.promise();

        // Read config and say after finish
        ConfigRetriever retriever = readFromFile(vertx, filename);

        retriever.getConfig(
                ar -> {
                    if (ar.failed()) {
                        // Failed to retrieve the configuration
//                        aHandler.handle(Future.failedFuture("Config not read"));
                        promise.fail(ar.cause());
                    } else {
                        // Success
                        //LOGGER.info("Config read: " + ar.result());
//                        aHandler.handle(Future.succeededFuture(ar.result()));
                        promise.complete(ar.result());
                    }
                });

        return promise.future();
    }

    static JsonObject dbConfig = null;

    public static JsonObject getPostgreConf(JsonObject dbConfigFromFile) {
        if (dbConfig == null) {
            dbConfig = new JsonObject()
                    .put("url", dbConfigFromFile.getString("url", "localhost"))
                    .put("driver_class", dbConfigFromFile.getString("driver", ""))
                    .put("max_pool_size", dbConfigFromFile.getInteger("maxPoolSize", 30))
                    .put("user", dbConfigFromFile.getString("username", "username"))
                    .put("password", dbConfigFromFile.getString("password", "password"));
        }
        return dbConfig;
    }

    private static ConfigRetriever readFromFile(Vertx vertx, String filename) {
        // Set file where to read configurations
        ConfigStoreOptions fileStore = new ConfigStoreOptions()
                .setType("file")
                .setFormat("json")
                .setConfig(new JsonObject().put("path", "./conf/" + filename + ".json"));

        ConfigRetrieverOptions options = new ConfigRetrieverOptions()
                .addStore(fileStore);

        // Read config and say after finish
        return ConfigRetriever.create(vertx, options);
    }
}
