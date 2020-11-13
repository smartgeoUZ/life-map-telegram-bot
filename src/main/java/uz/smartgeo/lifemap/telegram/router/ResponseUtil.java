package uz.smartgeo.lifemap.telegram.router;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import java.util.List;
import java.util.function.Function;

public class ResponseUtil {
    public static void respondWithCreated(RoutingContext routingContext, JsonObject content) {
        routingContext
                .response()
                .setStatusCode(200)
                .putHeader("content-type", "application/json; charset=utf-8")
                .putHeader("Location", routingContext.request().absoluteURI())
                .putHeader("Access-Control-Allow-Origin", "*")
                .putHeader("Access-Control-Allow-Headers", "Authorization")
                .putHeader("Access-Control-Expose-Headers", "*")
                .putHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE")
                .end(new JsonObject().put("result", content).putNull("error").toString());
    }

    public static void respondError(RoutingContext routingContext, String content) {
        routingContext
                .response()
                .setStatusCode(200)
                .putHeader("content-type", "application/json; charset=utf-8")
                .putHeader("Location", routingContext.request().absoluteURI())
                .putHeader("Access-Control-Allow-Origin", "*")
                .putHeader("Access-Control-Allow-Headers", "Authorization")
                .putHeader("Access-Control-Expose-Headers", "*")
                .putHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, DELETE, PATCH, HEAD")
                .end(new JsonObject().put("error", content).putNull("result").toString());
    }

}
