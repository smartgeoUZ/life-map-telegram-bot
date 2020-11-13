package uz.smartgeo.lifemap.telegram.router;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.sstore.LocalSessionStore;
import org.apache.log4j.Logger;

public class MainRouter {
    private static Logger LOGGER = Logger.getLogger(NotificationRouter.class);

//    private static JsonObject MainRouterConfig;

    public static Router createRouting(Vertx vertx, JsonObject config) {
        LOGGER.info("STARTING createRouting");
        final Router router = Router.router(vertx);
//        MainRouterConfig = config;
        router.route().handler(BodyHandler.create());

        LocalSessionStore localSessionStore = LocalSessionStore.create(vertx);
        router.route().handler(SessionHandler.create(localSessionStore));

        router.route().handler(io.vertx.ext.web.handler.CorsHandler.create("*")
                .allowedHeader("Authorization"));

        router.mountSubRouter("/notify", new NotificationRouter(vertx, config).getRouter());

//        SessionStore store = LocalSessionStore.create(vertx);
//        router.route().handler(SessionHandler.create(store));

        LOGGER.info("END createRouting");

        return router;
    }

}
