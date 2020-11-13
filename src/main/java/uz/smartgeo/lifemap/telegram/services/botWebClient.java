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
import uz.smartgeo.lifemap.telegram.BotConfig;
import uz.smartgeo.lifemap.telegram.utils.Emoji;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class botWebClient {
    protected WebClient webClient;
    protected WebClient webClientWithSSL;
    private static Logger LOGGER = Logger.getLogger(botWebClient.class);

    public botWebClient(Vertx vertx) {
        WebClientOptions options = new WebClientOptions()
                .setSsl(false)
                .setUserAgent("botWebClient/1.0.1");

        options.setKeepAlive(true);

        webClient = WebClient.create(vertx, options);
        webClientWithSSL = WebClient.create(vertx, options.setSsl(true));
    }

    public Future<JsonObject> sendMessage(int userId, String message) throws UnsupportedEncodingException {
        LOGGER.info("---sendMessage START---");
        Promise<JsonObject> promise = Promise.promise();

        String botToken = BotConfig.WEBHOOK_TOKEN;

        message = URLEncoder.encode(message, "UTF-8");

        webClient
                .get("api.telegram.org", "/bot" + botToken + "/sendMessage?chat_id=" + userId + "&text=" + message)
                .as(BodyCodec.jsonObject())

                .send(ar -> {
                    if (ar.succeeded()) {
                        promise.complete(ar.result().body());
                    } else {
                        LOGGER.info(ar);
                        promise.fail("DATA FAILED");
                        System.out.println("Something went wrong " + ar.cause().getMessage());
                        LOGGER.error("error in sendMessage. " + ar.cause().toString());
                    }
                });

        return promise.future();
    }

    public Future<JsonObject> sendAnswer2(String message) throws UnsupportedEncodingException {
        LOGGER.info("---sendMessage START---");
        Promise<JsonObject> promise = Promise.promise();

        try {
            String botToken = BotConfig.WEBHOOK_TOKEN;

            JsonArray results = new JsonArray();

            JsonObject inlineQueryResult1 = new JsonObject();
            inlineQueryResult1.put("type", "article");
            inlineQueryResult1.put("id", "1");
            inlineQueryResult1.put("title", "title");
            inlineQueryResult1.put("parse_mode", "Markdown");
            inlineQueryResult1.put("message_text", "blah blah");
//        inlineQueryResult1.put("description", "description");
//        inlineQueryResult1.put("reply_markup", "description");

            JsonObject inlineQueryResult2 = new JsonObject();
            inlineQueryResult2.put("type", "article");
            inlineQueryResult2.put("id", "2");
            inlineQueryResult2.put("title", "title2");
            inlineQueryResult2.put("parse_mode", "Markdown");
            inlineQueryResult2.put("message_text", "blah blah 2");

            results.add(inlineQueryResult1);
            results.add(inlineQueryResult2);

            JsonObject postParams = new JsonObject()
                    .put("inline_query_id", "1")
                    .put("results", results.encode());

//            message = URLEncoder.encode(message, "UTF-8");

//        .post("api.telegram.org", "/bot" + botToken + "/answerInlineQuery",postParams)

            webClient
                    .post("api.telegram.org", "/bot" + botToken + "/answerInlineQuery")
                    .as(BodyCodec.jsonObject())
                    .sendJsonObject(postParams, ar -> {
                        if (ar.succeeded()) {
                            promise.complete(ar.result().body());
                        } else {
                            LOGGER.info(ar);
                            promise.fail("DATA FAILED");
                            System.out.println("Something went wrong " + ar.cause().getMessage());
                            LOGGER.error("error in sendMessage. " + ar.cause().toString());
                        }
                    });

            return promise.future();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return promise.future();

    }

    public Future<JsonObject> sendMessageWithQuestionForModeration(int userId, long eventId, String baseMessage, String lang, String lifemapUrl) throws UnsupportedEncodingException {
        LOGGER.info("---sendQuestionForModeration START---");
        Promise<JsonObject> promise = Promise.promise();

        try {
            String botToken = BotConfig.WEBHOOK_TOKEN;

            JsonObject replyMarkup = new JsonObject();

            JsonArray inlineKeyboardRoot = new JsonArray();
            JsonArray inlineKeyboard = new JsonArray();

            String showEventOnMapText = String.format(LocalisationService.getString("ShowEventOnMap", lang),
                    Emoji.GLOBE_WITH_MERIDIANS.toString());
            JsonObject inlineKeyboardButtonShowEventOnMap = new JsonObject();
            inlineKeyboardButtonShowEventOnMap.put("text", showEventOnMapText);
            inlineKeyboardButtonShowEventOnMap.put("url", lifemapUrl + "#/event/" + eventId);

            String rejectMessageString = String.format(LocalisationService.getString("RejectEventModeration", lang),
                    Emoji.FAILED.toString());
            JsonObject inlineKeyboardButtonReject = new JsonObject();
            inlineKeyboardButtonReject.put("text", rejectMessageString);
            inlineKeyboardButtonReject.put("callback_data", eventId + "=" + "false");

            String approveMessageString = String.format(LocalisationService.getString("ApproveEventModeration", lang),
                    Emoji.SUCCESS.toString());

            JsonObject inlineKeyboardButtonApprove = new JsonObject();
            inlineKeyboardButtonApprove.put("text", String.format(approveMessageString, lang));
            inlineKeyboardButtonApprove.put("callback_data", eventId + "=" + "true");

            inlineKeyboard.add(inlineKeyboardButtonShowEventOnMap);
            inlineKeyboard.add(inlineKeyboardButtonReject);
            inlineKeyboard.add(inlineKeyboardButtonApprove);

            inlineKeyboardRoot.add(inlineKeyboard);

            replyMarkup.put("inline_keyboard", inlineKeyboardRoot);

//            LOGGER.info(replyMarkup);
//            LOGGER.info(replyMarkup.encode());
//            LOGGER.info(replyMarkup.encodePrettily());

            String messageText = baseMessage + "\n\n" + LocalisationService.getString("SetModerationStatus", lang) + "  - Event ID: " + eventId;

            JsonObject postParams = new JsonObject()
                    .put("chat_id", userId)
                    .put("text", messageText)
                    .put("reply_markup", replyMarkup.encode());


            LOGGER.info(postParams);
//            LOGGER.info(postParams.encode());

//            postParams = postParams.encode().tos;

            webClientWithSSL
                    .post(443, "api.telegram.org", "/bot" + botToken + "/sendMessage")
                    .putHeader("Accept", "application/json")
                    .putHeader("content-type", "application/json")
                    .putHeader("Access-Control-Allow-Origin", "*")
                    .putHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
                    .sendJsonObject(postParams, ar -> {
                        if (ar.succeeded()) {
                            promise.complete(new JsonObject().put("result", ar.result().body().toJsonObject()));
//                            promise.complete(ar.result().body().toJsonObject().getBoolean(""));
                        } else {
                            LOGGER.info(ar);
                            promise.fail("DATA FAILED");
                            System.out.println("Something went wrong " + ar.cause().getMessage());
                            LOGGER.error("error in sendMessage. " + ar.cause().toString());
                        }
                    });

            return promise.future();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return promise.future();
    }
}
