package uz.smartgeo.lifemap.telegram;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import org.apache.log4j.Logger;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;
import uz.smartgeo.lifemap.telegram.router.MainRouter;
import uz.smartgeo.lifemap.telegram.services.ConfigService;
import uz.smartgeo.lifemap.telegram.updateshandlers.WebHookHandler;
import uz.smartgeo.lifemap.telegram.updateshandlers.WebHookHandlerDev;

public class MainVerticle extends AbstractVerticle {

    HttpServer server;
    Router router;

    JsonObject botConfig = new JsonObject();
    JsonObject config = new JsonObject();

    private static final Logger LOGGER = Logger.getLogger(MainVerticle.class);

    @Override
    public void start() throws Exception {
        try {

            ConfigService.getConfig(vertx, "config")
                    .onComplete(configRes -> {
                        LOGGER.info("CONFIG LOAD COMPLETED");
                        config = configRes.result();

                        createNotificationServer();

                        ConfigService.getConfig(vertx, "bot-config")
                                .onComplete(botConfigRes -> {
                                    LOGGER.info("BOT CONFIG LOAD COMPLETED");
                                    botConfig = botConfigRes.result();

                                    startTelegramBotsApi();
                                })
                                .onFailure(botConfigRes -> {
                                    LOGGER.info("BOT CONFIG LOAD FAILED");
                                });
                    })
                    .onFailure(botConfigRes -> {
                        if (botConfigRes.getCause().getMessage() != null) {
                            LOGGER.info("CONFIG LOAD FAILED:" + botConfigRes.getCause().getMessage());
                        } else {
                            LOGGER.info("CONFIG LOAD FAILED:");
                        }

                    });

        } catch (Exception e) {
            if (e.getMessage() == null) {
                LOGGER.error("start Error: " + e);
            } else {
                LOGGER.error("start Error: " + e.getMessage());
            }
        }
    }

    private void startTelegramBotsApi() {
        try {
            LOGGER.info("Starting startTelegramBotsApi ...");

            ApiContextInitializer.init();

            String mode = botConfig.getString("MODE");
            String externalWebhookUrl = botConfig.getString("EXTERNAL_WEBHOOK_URL");
            String internalWebhookUrl = botConfig.getString("INTERNAL_WEBHOOK_URL");
            Integer internalWebhookPort = botConfig.getInteger("INTERNAL_WEBHOOK_PORT");

            TelegramBotsApi telegramBotsApi = null;
            try {
                if (mode.equals("dev")) {
                    telegramBotsApi = new TelegramBotsApi();
                } else {
                    telegramBotsApi = createTelegramBotsApi(externalWebhookUrl, internalWebhookUrl, internalWebhookPort);
                }

            } catch (TelegramApiException e) {
                LOGGER.error("ERROR on createTelegramBotsApi: " + e.getMessage());
                e.printStackTrace();
            }

            try {
                if (telegramBotsApi != null) {
                    LOGGER.info("telegramBotsApi registerBot");

                    if (mode.equals("prod")) {
                        telegramBotsApi.registerBot(new WebHookHandler(vertx, config));
                    } else {
                        telegramBotsApi.registerBot(new WebHookHandlerDev(vertx, config));
                    }

//                    telegramBotsApi.registerBot(new CommandsHandler(config));
//                    telegramBotsApi.registerBot(new WebHookHandlersDev(vertx, config));

                } else {
                    LOGGER.info("telegramBotsApi is NULL");
                }
            } catch (TelegramApiRequestException e) {
                if (e.getMessage() == null) {
                    LOGGER.error("ERROR on startTelegramBotsApi: " + e);
                } else {
                    LOGGER.error("ERROR on startTelegramBotsApi: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            if (e.getMessage() == null) {
                LOGGER.error("ERROR on startTelegramBotsApi: " + e);
            } else {
                LOGGER.error("ERROR on startTelegramBotsApi: " + e.getMessage());
            }
        }
    }

    /**
     * Start notifications listener
     */
    private void createNotificationServer() {
        try {
            LOGGER.info("Starting createNotificationServer ...");

            router = MainRouter.createRouting(vertx, config);

            LOGGER.info("Starting createHttpServer ...");
            server = vertx.createHttpServer();
            LOGGER.info("END createHttpServer ...");

            LOGGER.info("STARTING LISTENER SERVER: " + config.getString("server.host") + ":" + config.getInteger("server.port"));
            server
                    .requestHandler(router::accept)
                    .listen(config.getInteger("server.port", 8779),
                            config.getString("server.host", "localhost"), r -> {
                                LOGGER.info("LISTENER SERVER STARTED");

                                if (r.cause() != null && r.cause().getMessage() != null) {
                                    LOGGER.info(r.cause());
                                    LOGGER.error(r.cause().getMessage());
                                }

                                if (r.succeeded()) {
                                    LOGGER.info("Http server started on :" + server.actualPort());
                                } else {
                                    LOGGER.error("Http server start failed -> " + r.cause().getMessage());
                                }
                            });

        } catch (Exception e) {
            if (e.getMessage() == null) {
                LOGGER.error("ERROR in createNotificationServer: " + e);
            } else {
                LOGGER.error("ERROR in createNotificationServer: " + e.getMessage());
            }

        }
    }

    private static TelegramBotsApi createTelegramBotsApi(String externalWebHookUrl, String internalWebHookUrl, int internalWebHookPort) throws
            TelegramApiException {
        return new TelegramBotsApi(externalWebHookUrl, internalWebHookUrl + internalWebHookPort);
    }


}
//   return new TelegramBotsApi(BuildVars.EXTERNALWEBHOOKURL, BuildVars.INTERNALWEBHOOKURL);
