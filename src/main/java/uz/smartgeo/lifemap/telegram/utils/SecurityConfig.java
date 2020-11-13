package uz.smartgeo.lifemap.telegram.utils;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.CSRFHandler;
import io.vertx.ext.web.handler.CookieHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.sstore.LocalSessionStore;
import org.apache.log4j.Logger;

public class SecurityConfig {

    private static final Logger LOGGER = Logger.getLogger(SecurityConfig.class);

    public static void addSecurity(Router router, Vertx vertx, JsonObject config) {

        if (config.getBoolean("vertx.development.mode", false)) {
            LOGGER.info("SecurityConfig development mode");
        } else {
            LOGGER.info("SecurityConfig Production mode");
            // Sessions
            router.route().handler(CookieHandler.create());
            router.route().handler(
                    SessionHandler
                            .create(LocalSessionStore.create(vertx))
                            .setCookieHttpOnlyFlag(true)
                            .setCookieSecureFlag(true));
            // CSRF protection
            router.route().handler(CSRFHandler.create("not a good secret"));
            // Headers
            router.route().handler(addSecurityHeaders());
        }

    }

    private static Handler<RoutingContext> addSecurityHeaders() {
        return (RoutingContext rc) -> {
            final HttpServerResponse response = rc.response();
            // prevent caching for HTTP/1.1
            response.putHeader("Cache-Control", "no-store, no-cache, must-revalidate");
            // prevent caching for HTTP/1.0
            response.putHeader("Pragma", "no-cache").putHeader("Expires", "0");
            // prevents Internet Explorer from MIME - sniffing a
            // response away from the declared content-type
            response.putHeader("X-Content-Type-Options", "nosniff");
            // Strict HTTPS (for about ~6Months)
            response.putHeader("Strict-Transport-Security", "max-age=15768000 ; includeSubDomains");
            // IE8+ do not allow opening of attachments in the context
            // of this resource
            response.putHeader("X-Download-Options", "noopen");
            // enable XSS protection for IE
            response.putHeader("X-XSS-Protection", "1; mode=block");
            // deny frames
            response.putHeader("X-FRAME-OPTIONS", "DENY");

            rc.next();
        };
    }
}
