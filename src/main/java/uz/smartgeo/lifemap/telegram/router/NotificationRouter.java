package uz.smartgeo.lifemap.telegram.router;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import uz.smartgeo.lifemap.telegram.database.DatabaseManager;
import uz.smartgeo.lifemap.telegram.services.LifeMapApi;
import uz.smartgeo.lifemap.telegram.services.LocalisationService;
import uz.smartgeo.lifemap.telegram.services.botWebClient;
import uz.smartgeo.lifemap.telegram.utils.AuthUtils;
import uz.smartgeo.lifemap.telegram.utils.Converters;
import uz.smartgeo.lifemap.telegram.utils.LIFEMAP_CONST;

import java.util.List;

import static uz.smartgeo.lifemap.telegram.utils.LIFEMAP_CONST.DEFAULT_LANGUAGE;

public class NotificationRouter {
    private static final Logger LOGGER = Logger.getLogger(NotificationRouter.class);

    protected Router router;
    protected botWebClient botWebClient;
    protected LifeMapApi lifeMapApi;

    JsonObject config;

    private static final String DEFAULT_LANG = "en";

    public NotificationRouter(Vertx vertx, JsonObject config) {
        try {
            this.config = config;

            this.router = Router.router(vertx);

            router.post("/protected/*").handler(this::authHandler);
            router.get("/protected/*").handler(this::authHandler);

            router.post("/eventAdded").handler(this::eventAdded);
            router.post("/complaintToEvent").handler(this::complaintToEvent);

            router.post("/eventResponsed").handler(this::eventResponsed);
            router.post("/eventResponsedFromBirdamlik").handler(this::eventResponsedFromBirdamlik);
            router.post("/eventModerated").handler(this::eventModerated);
            router.post("/userWantCommunication").handler(this::userWantCommunication);

            this.router.route().handler(BodyHandler.create());

            botWebClient = new botWebClient(vertx);
            createLifeMapApiWebClient(vertx);
        } catch (Exception e) {
            LOGGER.error("ERROR in NotificationRouter: " + e.getMessage());
        }
    }

    private void authHandler(RoutingContext context) {
        final String authorization = context.request().getHeader(HttpHeaders.AUTHORIZATION);

        if (AuthUtils.tokenIsValid(authorization)) {
            context.next();
        } else {
            ResponseUtil.respondError(context, "Not authorized request");
        }
    }

    private void createLifeMapApiWebClient(Vertx vertx) {
        ConfigRetrieverOptions lmApiConfigRetrieverOptions = new ConfigRetrieverOptions()
                .addStore(new ConfigStoreOptions().setType("file").setConfig(
                        new JsonObject().put("path", "conf/lm-config.json")));

        ConfigRetriever lmApiRetriever = ConfigRetriever.create(vertx, lmApiConfigRetrieverOptions);

        LOGGER.info("lm-config load start");

        lmApiRetriever.getConfig(lifeMapApiConfigRes -> {
            LOGGER.info("lm-config load success");

            JsonObject lifeMapApiConf = lifeMapApiConfigRes.result();

            String host = lifeMapApiConf.getString("server.host");
            Integer port = lifeMapApiConf.getInteger("server.port");

            lifeMapApi = new LifeMapApi(vertx, host, port);
        });
    }

    private void eventAdded(RoutingContext context) {
        JsonObject formData = context.getBodyAsJson();

        if (formData != null) {
            if (formData.containsKey("user_id") && formData.getLong("user_id", null) != null &&
                    formData.containsKey("ext_user_id") && formData.getLong("ext_user_id", null) != null &&
                    formData.containsKey("id") && formData.getLong("id", null) != null &&
                    formData.containsKey("category_id") && formData.getInteger("category_id", null) != null &&
                    formData.containsKey("name") && formData.getString("name", null) != null &&
                    formData.containsKey("address") && formData.getString("address", null) != null &&
                    formData.containsKey("start_date") && formData.getString("start_date", null) != null &&
                    formData.containsKey("reg_date") && formData.getString("reg_date", null) != null &&
                    formData.containsKey("end_date") && formData.getString("end_date", null) != null &&
                    formData.containsKey("ext_user_name") && formData.getString("ext_user_name", null) != null &&
                    formData.containsKey("region_id") && formData.getLong("region_id", null) != null &&
                    formData.containsKey("moderation_status") && formData.getInteger("moderation_status", null) != null
            ) {
                Integer extUserId = formData.getInteger("ext_user_id");
                String lang = getUserLang(extUserId);
//                System.out.println(LocalisationService.getString("eventCategoryFood", lang));

                String extUserLastName = "";
                String extUserLogin = "";
                String categoryName;


                Long eventId = formData.getLong("id");
                Integer categoryId = formData.getInteger("category_id");
                String eventName = formData.getString("name");
                String description = formData.getString("description");
                JSONObject addressJson = new JSONObject(formData.getString("address"));
                String address = addressJson.getString("display_name");
                String startDateISO = formData.getString("start_date");
                String regDateISO = formData.getString("reg_date");
                String endDateISO = formData.getString("end_date");
                String extUserName = formData.getString("ext_user_name");
                Integer moderationStatus = formData.getInteger("moderation_status");
                String endDate = Converters.getDateTimeFromISO(endDateISO);
                String startDate = Converters.getDateTimeFromISO(startDateISO);
                String regDate = Converters.getDateTimeFromISO(regDateISO);

                categoryName = LocalisationService.getCategoryNameById(categoryId, lang);
                if (formData.containsKey("ext_user_last_name")) {
                    extUserLastName = formData.getString("ext_user_last_name");
                }

                if (formData.containsKey("ext_user_login")) {
                    extUserLogin = formData.getString("ext_user_login");
                }

                String M = "";

                if (moderationStatus == 1) {
                    M = " " + LocalisationService.getString("createdEventForModeration", lang);
                } else if (moderationStatus == 2) {
                    M = " " + LocalisationService.getString("haveChangedEventForModeration", lang);
                } else if (moderationStatus == 3) {
                    M = " " + LocalisationService.getString("haveChangedPublishedEventForModeration", lang);
                }
                String aboutNewEventMessage = LocalisationService.getString("User", lang) + ": " + extUserName + " " + extUserLastName + " @" + extUserLogin + M + "\n" +
                        "-------------------" + "\n" +
                        LocalisationService.getString("EventID", lang) + ": " + eventId + "\n" +
                        LocalisationService.getString("Category", lang) + ":  " + categoryName + "\n" +
                        LocalisationService.getString("EventTopic", lang) + ":  " + eventName + "\n" +
                        LocalisationService.getString("EventDescription", lang) + ":  " + description + "\n" + "\n" +
                        LocalisationService.getString("DateCreated", lang) + ":  " + regDate + "\n" +
                        LocalisationService.getString("DateStart", lang) + ":  " + startDate + "\n" +
                        LocalisationService.getString("DateEnd", lang) + ":  " + endDate + "\n" +
                        LocalisationService.getString("ForModeration", lang) + "\n" +
                        "---------------------" + "\n" +
                        LocalisationService.getString("Address", lang) + ": " + address;

                // Get moderators from DB
                lifeMapApi.getUsersByRoleId(LIFEMAP_CONST.ROLE_MODERATOR)
                        .onComplete(res -> {
                            // Send message for each user
                            int index = 0;

                            for (Object userObj : res.result()) {
                                if (userObj instanceof JsonObject) {
                                    JsonObject user = (JsonObject) userObj;
                                    Long userRegionId = user.getLong("region_id");
                                    int extUserIdForSend = user.getInteger("ext_user_id");
                                    Long eventRegionId = formData.getLong("region_id") != null ? formData.getLong("region_id") : 1L;
                                    if (userRegionId == null) {
                                        userRegionId = 1L;
                                    }

                                    if ((eventRegionId == 1 || userRegionId == 1) || eventRegionId.equals(userRegionId)) {
                                        try {
                                            // Send message to moderator in cycle
                                            String lifeMapUrl = this.config.getString("lifemap.url");

                                            if (index == res.result().size() - 1) {
                                                botWebClient.sendMessageWithQuestionForModeration(
                                                        extUserIdForSend,
                                                        eventId,
                                                        aboutNewEventMessage,
                                                        lang,
                                                        lifeMapUrl)
                                                        .onComplete(sendMessageHandler(context));
                                            } else {
                                                botWebClient.sendMessageWithQuestionForModeration(
                                                        extUserIdForSend,
                                                        eventId,
                                                        aboutNewEventMessage,
                                                        lang,
                                                        lifeMapUrl);
                                            }

                                            index++;
                                        } catch (Exception e) {
                                            LOGGER.error("Error in eventAdded: " + e.getMessage());
                                            e.printStackTrace();
                                        }
                                    }
                                    LOGGER.info(user.getLong("ext_user_id"));
                                }
                            }
                        })
                        .onFailure(res -> {
                            if (res.getCause().getMessage() != null) {
                                ResponseUtil.respondError(context, res.getCause().getMessage());
                                LOGGER.error("Request failed -> " + res.getCause().getMessage());
                            } else {
                                ResponseUtil.respondError(context, res.getCause().toString());
                                LOGGER.error("Request failed -> " + res.getCause());
                            }
                        });


            } else {
                ResponseUtil.respondError(context, "Not found some post data");
            }
        } else {
            ResponseUtil.respondError(context, "No data for fields");
        }
    }

    private void complaintToEvent(RoutingContext context) {
        JsonObject formData = context.getBodyAsJson();

        if (formData != null) {
            if (formData.containsKey("userId") && formData.getLong("userId", null) != null &&
                    formData.containsKey("userExtId") && formData.getLong("userExtId", null) != null &&
                    formData.containsKey("categoryId") && formData.getInteger("categoryId", null) != null &&
                    formData.containsKey("name") && formData.getString("name", null) != null &&
                    formData.containsKey("address") && formData.getString("address", null) != null &&
                    formData.containsKey("startDate") && formData.getString("startDate", null) != null &&
                    formData.containsKey("regDate") && formData.getString("regDate", null) != null &&
                    formData.containsKey("endDate") && formData.getString("endDate", null) != null &&
                    formData.containsKey("userFirstName") && formData.getString("userFirstName", null) != null
            ) {
                Integer extUserId = formData.getInteger("userExtId");
                String lang = getUserLang(extUserId);
                System.out.println(LocalisationService.getString("eventCategoryFood", lang));

                String responsedUserFirstName = "";
                String responsedUserLastName = "";
                String responsedUserLogin = "";

                String extUserLastName = "";
                String extUserLogin = "";
                String categoryName;

                Long eventId = formData.getLong("eventId");
                Integer categoryId = formData.getInteger("categoryId");
                String eventName = formData.getString("name");
                String description = formData.getString("message");
                String address = formData.getString("address");
                String startDateISO = formData.getString("startDate");
                String regDateISO = formData.getString("regDate");
                String endDateISO = formData.getString("endDate");
//                String extUserName = formData.getString("userFirstName");
                String endDate = Converters.getDateTimeFromISO(endDateISO);
                String startDate = Converters.getDateTimeFromISO(startDateISO);
                String regDate = Converters.getDateTimeFromISO(regDateISO);
                categoryName = LocalisationService.getCategoryNameById(categoryId, lang);
                Boolean isModerated = formData.getBoolean("isModerated");

//                if (formData.containsKey("userLastName")) {
//                    extUserLastName = formData.getString("userLastName");
//                }
//
//                if (formData.containsKey("userLogin")) {
//                    extUserLogin = formData.getString("userLogin");
//                }

                if (formData.containsKey("responsedUserFirstName")) {
                    responsedUserFirstName = formData.getString("responsedUserFirstName");
                }

                if (formData.containsKey("responsedUserLastName")) {
                    responsedUserLastName = formData.getString("responsedUserLastName");
                }

                if (formData.containsKey("responsedUserLogin")) {
                    responsedUserLogin = formData.getString("responsedUserLogin");
                }

                String moderationStatus;

                if (isModerated == null) {
                    moderationStatus = " " + LocalisationService.getString("NotModerated", lang);
                } else if (isModerated) {
                    moderationStatus = " " + LocalisationService.getString("Moderated", lang);
                } else //noinspection ConstantConditions
                    if (!isModerated) {
                        moderationStatus = " " + LocalisationService.getString("NotPassedModeration", lang);
                    } else {
                        moderationStatus = "  " + LocalisationService.getString("NotModerated", lang);
                    }

                String aboutNewEventMessage = LocalisationService.getString("User", lang) + ": " + responsedUserFirstName + " " + responsedUserLastName + " @" + responsedUserLogin + " " + LocalisationService.getString("haveComplainedOnIncorrectMessage", lang) + " :" + "\n" +
                        "-------------------" + "\n" +
                        LocalisationService.getString("EventID", lang) + ": " + eventId + "\n" +
                        LocalisationService.getString("Category", lang) + ":  " + categoryName + "\n" +
                        LocalisationService.getString("EventTopic", lang) + ":  " + eventName + "\n" +
                        LocalisationService.getString("EventDescription", lang) + ":  " + description + "\n" + "\n" +
                        LocalisationService.getString("DateCreated", lang) + ":  " + regDate + "\n" +
                        LocalisationService.getString("DateStart", lang) + ":  " + startDate + "\n" +
                        LocalisationService.getString("DateEnd", lang) + ":  " + endDate + "\n" +
                        LocalisationService.getString("Status", lang) + ":  " + moderationStatus + "\n" +
                        "---------------------" + "\n" +
                        LocalisationService.getString("Address", lang) + ": " + address;

                // Get moderators from DB
                lifeMapApi.getUsersByRoleId(LIFEMAP_CONST.ROLE_ADMINISTRATOR)
                        .onComplete(res -> {

                            // Send message for each user
                            int index = 0;

                            for (Object userObj : res.result()) {
                                if (userObj instanceof JsonObject) {
                                    JsonObject user = (JsonObject) userObj;

                                    LOGGER.info(user.getLong("ext_user_id"));
                                    int extUserIdForSend = user.getInteger("ext_user_id");

                                    try {

                                        // Send message to administrator in cycle
                                        if (index == res.result().size() - 1) {
                                            botWebClient.sendMessage(extUserIdForSend, aboutNewEventMessage).onComplete(sendMessageHandler(context));
                                        } else {
                                            botWebClient.sendMessage(extUserIdForSend, aboutNewEventMessage);
                                        }

                                        index++;
                                    } catch (Exception e) {
                                        LOGGER.error("Error in eventAdded: " + e.getMessage());
                                        e.printStackTrace();
                                    }

                                }

                            }

                        })
                        .onFailure(res -> {
                            if (res.getCause().getMessage() != null) {
                                ResponseUtil.respondError(context, res.getCause().getMessage());
                                LOGGER.error("Request failed -> " + res.getCause().getMessage());
                            } else {
                                ResponseUtil.respondError(context, res.getCause().toString());
                                LOGGER.error("Request failed -> " + res.getCause());
                            }
                        });


            } else {
                ResponseUtil.respondError(context, "Not found some post data");
            }
        } else {
            ResponseUtil.respondError(context, "No data for fields");
        }
    }

    private void eventResponsed(RoutingContext context) {
        JsonObject formData = context.getBodyAsJson();

        if (formData != null) {
            if (formData.containsKey("userId") && formData.getLong("userId") != null &&
                    formData.containsKey("userExtId") && formData.getLong("userExtId") != null &&
                    formData.containsKey("eventId") && formData.getLong("eventId") != null &&
                    formData.containsKey("startDate") && formData.getString("startDate") != null &&
                    formData.containsKey("responsedUserId") && formData.getLong("responsedUserId") != null) {

                Integer extUserId = formData.getInteger("userExtId");
                String lang = getUserLang(extUserId);
//                System.out.println(LocalisationService.getString("eventCategoryFood", lang));

                String categoryName;

                String eventName = formData.getString("name");
                String eventDescription = formData.getString("message");

                Long eventId = formData.getLong("eventId");
                String responsedUserFirstName = formData.getString("responsedUserFirstName");
                String responsedUserLastName = "";
                String responsedUserLogin = "";

                Integer categoryId = formData.getInteger("categoryId");
                String startDateISO = formData.getString("startDate");
                String regDateISO = formData.getString("regDate");
                String endDateISO = formData.getString("endDate");
                String address = formData.getString("address");

                String endDate = Converters.getDateTimeFromISO(endDateISO);
                String regDate = Converters.getDateTimeFromISO(regDateISO);
                String startDate = Converters.getDateTimeFromISO(startDateISO);

                categoryName = LocalisationService.getCategoryNameById(categoryId, lang);

                if (formData.containsKey("name")) {
                    eventName = formData.getString("name");
                }
                if (formData.containsKey("message")) {
                    eventDescription = formData.getString("message");
                }

                if (formData.containsKey("responsedUserLastName")) {
                    responsedUserLastName = formData.getString("responsedUserLastName");
                }

                if (formData.containsKey("responsedUserLogin")) {
                    responsedUserLogin = formData.getString("responsedUserLogin");
                }

                String aboutEventResponseMessage = LocalisationService.getString("User", lang) + " " + responsedUserFirstName + " " + responsedUserLastName + " @" + responsedUserLogin + " " +
                        "  " + LocalisationService.getString("respondedToYourEvent", lang) + "\n" +
                        "-------------------" + "\n" +
                        LocalisationService.getString("EventID", lang) + ": " + eventId + "\n" +
                        LocalisationService.getString("Category", lang) + ":  " + categoryName + "\n" +
                        LocalisationService.getString("EventTopic", lang) + ":  " + eventName + "\n" +
                        LocalisationService.getString("EventDescription", lang) + ":  " + eventDescription + "\n" + "\n" +
                        LocalisationService.getString("DateCreated", lang) + ":  " + regDate + "\n" +
                        LocalisationService.getString("DateStart", lang) + ":  " + startDate + "\n" +
                        LocalisationService.getString("DateEnd", lang) + ":  " + endDate + "\n" +
                        "---------------------" + "\n" +
                        LocalisationService.getString("Address", lang) + ": " + address + "\n"
                        + LocalisationService.getString("YouCanContactThis", lang);

                // Get user by telegram id
                lifeMapApi.getUserByExtId(extUserId)
                        .onComplete(res -> {
                            LOGGER.info(res);
                            JsonObject user = res.result().getJsonObject("result");

                            // Get user external id
                            if (user.containsKey("ext_user_id") && user.getLong("ext_user_id") != null) {
                                LOGGER.info(user.getLong("ext_user_id"));
                                int extUserIdForSend = user.getInteger("ext_user_id");

                                try {
                                    // Send message about response
                                    botWebClient.sendMessage(extUserIdForSend, aboutEventResponseMessage).onComplete(sendMessageHandler(context));
                                } catch (Exception e) {
                                    LOGGER.error("Error in eventResponsed: " + e.getMessage());
                                    e.printStackTrace();
                                }
                            }
                        })
                        .onFailure(res -> {
                            if (res.getCause().getMessage() != null) {
                                ResponseUtil.respondError(context, res.getCause().getMessage());
                                LOGGER.error("Request failed -> " + res.getCause().getMessage());
                            } else {
                                ResponseUtil.respondError(context, res.getCause().toString());
                                LOGGER.error("Request failed -> " + res.getCause());
                            }
                        });


            } else {
                ResponseUtil.respondError(context, "Not found some post data");
            }
        } else {
            ResponseUtil.respondError(context, "No data for fields");
        }
    }

    /**
     * Create response to event from birdamlik user account (id = 2) (Trusted User)
     * when Life Map event accepted in birdamlik.uz and event is assigned to the volunteer
     *
     * @param context RoutingContext
     */
    private void eventResponsedFromBirdamlik(RoutingContext context) {
        JsonObject formData = context.getBodyAsJson();

        if (formData != null) {
            if (formData.containsKey("userId") && formData.getLong("userId") != null &&
                    formData.containsKey("userExtId") && formData.getLong("userExtId") != null &&
                    formData.containsKey("eventId") && formData.getLong("eventId") != null &&
                    formData.containsKey("startDate") && formData.getString("startDate") != null &&
                    formData.containsKey("responsedUserId") && formData.getLong("responsedUserId") != null) {

                Integer extUserId = formData.getInteger("userExtId");
                String lang = getUserLang(extUserId);
//                System.out.println(LocalisationService.getString("eventCategoryFood", lang));

                String categoryName;

                String eventName = formData.getString("name");
                String eventDescription = formData.getString("message");
                Long eventId = formData.getLong("eventId");
                Long birdamlikPostId = formData.getLong("birdamlikPostId");
                String volunteerPhoneNumber = formData.getString("responsedUserPhoneMobile");
                String responsedUserFirstName = formData.getString("responsedUserFirstName");
                Integer categoryId = formData.getInteger("categoryId");
                String startDateISO = formData.getString("startDate");
                String regDateISO = formData.getString("regDate");
                String endDateISO = formData.getString("endDate");
                String address = formData.getString("address");

                String endDate = Converters.getDateTimeFromISO(endDateISO);
                String regDate = Converters.getDateTimeFromISO(regDateISO);
                String startDate = Converters.getDateTimeFromISO(startDateISO);

                categoryName = LocalisationService.getCategoryNameById(categoryId, lang);

                if (formData.containsKey("name")) {
                    eventName = formData.getString("name");
                }
                if (formData.containsKey("message")) {
                    eventDescription = formData.getString("message");
                }

                if (formData.containsKey("responsedUserPhoneMobile")) {
                    volunteerPhoneNumber = formData.getString("responsedUserPhoneMobile");
                }

                String aboutEventResponseMessage = LocalisationService.getString("Volunteer", lang) + " " + responsedUserFirstName + " " +
                        LocalisationService.getString("fromBirdamlikProject", lang) + "\n" +
                        LocalisationService.getString("respondedToYourEvent", lang) + " " + "\n" +
                        "-------------------" + "\n" +
                        LocalisationService.getString("EventID", lang) + ": " + eventId + "\n" +
                        LocalisationService.getString("BirdamlikPostID", lang) + ": " + birdamlikPostId + "\n" +
                        LocalisationService.getString("VolunteerPhone", lang) + ":  " + volunteerPhoneNumber + "\n" +
                        LocalisationService.getString("Category", lang) + ":  " + categoryName + "\n" +
                        LocalisationService.getString("EventTopic", lang) + ":  " + eventName + "\n" +
                        LocalisationService.getString("EventDescription", lang) + ":  " + eventDescription + "\n" + "\n" +
                        LocalisationService.getString("DateCreated", lang) + ":  " + regDate + "\n" +
                        LocalisationService.getString("DateStart", lang) + ":  " + startDate + "\n" +
                        LocalisationService.getString("DateEnd", lang) + ":  " + endDate + "\n" +
                        "---------------------" + "\n" +
                        LocalisationService.getString("Address", lang) + ": " + address + "\n"
                        + LocalisationService.getString("YouCanContactThisUserByPhone", lang);

                // Get user by telegram id
                lifeMapApi.getUserByExtId(extUserId)
                        .onComplete(res -> {
                            LOGGER.info(res);
                            JsonObject user = res.result().getJsonObject("result");

                            // Get user external id
                            if (user.containsKey("ext_user_id") && user.getLong("ext_user_id") != null) {
                                LOGGER.info(user.getLong("ext_user_id"));
                                int extUserIdForSend = user.getInteger("ext_user_id");

                                try {
                                    // Send message about birdamlik user response
                                    botWebClient.sendMessage(extUserIdForSend, aboutEventResponseMessage).onComplete(sendMessageHandler(context));
                                } catch (Exception e) {
                                    LOGGER.error("Error in eventResponsed: " + e.getMessage());
                                    e.printStackTrace();
                                }
                            }
                        })
                        .onFailure(res -> {
                            if (res.getCause().getMessage() != null) {
                                ResponseUtil.respondError(context, res.getCause().getMessage());
                                LOGGER.error("Request failed -> " + res.getCause().getMessage());
                            } else {
                                ResponseUtil.respondError(context, res.getCause().toString());
                                LOGGER.error("Request failed -> " + res.getCause());
                            }
                        });


            } else {
                ResponseUtil.respondError(context, "Not found some post data");
            }
        } else {
            ResponseUtil.respondError(context, "No data for fields");
        }
    }

    private void eventModerated(RoutingContext context) {
        JsonObject formData = context.getBodyAsJson();

        if (formData != null) {
            if (formData.containsKey("eventId") && formData.getLong("eventId") != null &&
                    formData.containsKey("userId") && formData.getLong("userId") != null &&
                    formData.containsKey("extUserId") && formData.getLong("extUserId") != null &&
                    formData.containsKey("categoryId") && formData.getLong("categoryId") != null &&
                    formData.containsKey("name") && formData.getString("name") != null &&
                    formData.containsKey("description") && formData.getString("description") != null &&
                    formData.containsKey("regDate") && formData.getString("regDate") != null &&
                    formData.containsKey("startDate") && formData.getString("startDate") != null &&
                    formData.containsKey("isModerationSuccess") && formData.getBoolean("isModerationSuccess") != null &&
                    formData.containsKey("endDate") && formData.getString("endDate") != null && formData.getString("address") != null) {

                Integer extUserId = formData.getInteger("extUserId");
                String lang = getUserLang(extUserId);
//                System.out.println(LocalisationService.getString("eventCategoryFood", lang));

                String categoryName;

                long eventId = formData.getLong("eventId");
                int categoryId = formData.getInteger("categoryId");
                String name = formData.getString("name");
                String description = formData.getString("description");
                String regDateISO = formData.getString("regDate");
                String startDateISO = formData.getString("startDate");
                String address = formData.getString("address");
                String startDate = Converters.getDateTimeFromISO(startDateISO);
                String regDate = Converters.getDateTimeFromISO(regDateISO);
                String endDate = Converters.getDateTimeFromISO(startDateISO);

                Boolean isModerationSuccess = formData.getBoolean("isModerationSuccess");
//
//                LOGGER.info(eventId);
//                LOGGER.info(userId);
//                LOGGER.info(extUserId);
//                LOGGER.info(categoryId);
//                LOGGER.info(name);
//                LOGGER.info(description);
//                LOGGER.info(regDate);
//                LOGGER.info(startDate);
//                LOGGER.info(endDate);
//                LOGGER.info(isModerationSuccess);

                categoryName = LocalisationService.getCategoryNameById(categoryId, lang);

                String M;
                if (isModerationSuccess) {
                    M = LocalisationService.getString("YourEventAccepted", lang);
                } else {
                    M = LocalisationService.getString("YourEventDeclined", lang);
                }

                String aboutModeratedMessage = M + "\n" +
                        "-------------------" + "\n" +
                        LocalisationService.getString("EventID", lang) + ": " + eventId + "\n" +
                        LocalisationService.getString("Category", lang) + ":  " + categoryName + "\n" +
                        LocalisationService.getString("EventTopic", lang) + ":  " + name + "\n" +
                        LocalisationService.getString("EventDescription", lang) + ":  " + description + "\n" + "\n" +
                        LocalisationService.getString("DateCreated", lang) + ":  " + regDate + "\n" +
                        LocalisationService.getString("DateStart", lang) + ":  " + startDate + "\n" +
                        LocalisationService.getString("DateEnd", lang) + ":  " + endDate + "\n" +
                        "---------------------" + "\n" +
                        LocalisationService.getString("Address", lang) + ": " + address + "\n";


                // Get user by telegram id
                lifeMapApi.getUserByExtId(extUserId)
                        .onComplete(res -> {
                            LOGGER.info(res);
                            JsonObject user = res.result().getJsonObject("result");

                            // Get user external id
                            if (user.containsKey("ext_user_id") && user.getLong("ext_user_id") != null) {
                                LOGGER.info(user.getLong("ext_user_id"));
                                int extUserIdForSend = user.getInteger("ext_user_id");

                                try {
                                    // Send message about moderation status changed
                                    botWebClient.sendMessage(extUserIdForSend, aboutModeratedMessage).onComplete(sendMessageHandler(context));
                                } catch (Exception e) {
                                    LOGGER.error("Error in eventModerated: " + e.getMessage());
                                    e.printStackTrace();
                                }
                            }
                        })
                        .onFailure(res -> {
                            if (res.getCause().getMessage() != null) {
                                ResponseUtil.respondError(context, res.getCause().getMessage());
                                LOGGER.error("Request failed -> " + res.getCause().getMessage());
                            } else {
                                ResponseUtil.respondError(context, res.getCause().toString());
                                LOGGER.error("Request failed -> " + res.getCause());
                            }
                        });


            } else {
                ResponseUtil.respondError(context, "Not found some post data");
            }

        }
    }

    private void userWantCommunication(RoutingContext context) {
        JsonObject formData = context.getBodyAsJson();

        if (formData != null) {
            if (formData.containsKey("userId") && formData.getLong("userId") != null &&
                    formData.containsKey("userExtId") && formData.getLong("userExtId") != null &&
                    formData.containsKey("eventId") && formData.getLong("eventId") != null &&
                    formData.containsKey("startDate") && formData.getString("startDate") != null &&
                    formData.containsKey("responsedUserId") && formData.getLong("responsedUserId") != null) {

                Integer extUserId = formData.getInteger("userExtId");
                String lang = getUserLang(extUserId);
//                System.out.println(LocalisationService.getString("eventCategoryFood", lang));

                String categoryName;

                String eventName = formData.getString("name");
                String eventDescription = formData.getString("message");
//                String userFirstName = formData.getString("userFirstName");
//                String userLastName = "";
//                String userLogin = "";
//                Long userId = formData.getLong("userId");
                Long eventId = formData.getLong("eventId");
//                Long responsedUserId = formData.getLong("responsedUserId");
                String responsedUserFirstName = formData.getString("responsedUserFirstName");
                String responsedUserLastName = "";
                String responsedUserLogin = "";
//                String responsedUserEmail = "";
                String responsedUserPhoneMobile = formData.getString("responsedUserMobileNumber");

                Integer categoryId = formData.getInteger("categoryId");
                String startDateISO = formData.getString("startDate");
                String regDateISO = formData.getString("regDate");
                String endDateISO = formData.getString("endDate");
                String address = formData.getString("address");

                String endDate = Converters.getDateTimeFromISO(endDateISO);
                String regDate = Converters.getDateTimeFromISO(regDateISO);
                String startDate = Converters.getDateTimeFromISO(startDateISO);

                categoryName = LocalisationService.getCategoryNameById(categoryId, lang);

                //  String startDateLocale = getDateTimeFromISO(startDateLocaleISO);

                if (formData.containsKey("name")) {
                    eventName = formData.getString("name");
                }
                if (formData.containsKey("message")) {
                    eventDescription = formData.getString("message");
                }
//                if (formData.containsKey("userEmail")) {
//                    userEmail = formData.getString("userEmail");
//                }


//                if (formData.containsKey("userLastName")) {
//                    userLastName = formData.getString("userLastName");
//                }
//                if (formData.containsKey("userPhoneMobile")) {
//                    userPhoneMobile = formData.getString("userPhoneMobile");
//                }
//                if (formData.containsKey("userPhotoUrl")) {
//                    userPhotoUrl = formData.getString("userPhotoUrl");
//                }
//                if (formData.containsKey("userLogin")) {
//                    userLogin = formData.getString("userLogin");
//                }

                if (formData.containsKey("responsedUserLastName")) {
                    responsedUserLastName = formData.getString("responsedUserLastName");
                }

                if (formData.containsKey("responsedUserLogin")) {
                    responsedUserLogin = formData.getString("responsedUserLogin");
                }

                if (formData.containsKey("responsedUserLogin")) {
                    responsedUserLogin = formData.getString("responsedUserLogin");
                }

                if (formData.containsKey("responsedUserLogin")) {
                    responsedUserLogin = formData.getString("responsedUserLogin");
                }

//                if (formData.containsKey("responsedUserEmail")) {
//                    responsedUserEmail = formData.getString("responsedUserEmail");
//                }

                if (formData.containsKey("responsedUserMobileNumber")) {
                    responsedUserPhoneMobile = formData.getString("responsedUserMobileNumber");
                }

                String aboutEventResponseMessage = LocalisationService.getString("User", lang) + " " + responsedUserFirstName + " " + responsedUserLastName + " @" + responsedUserLogin + " (+" +
                        responsedUserPhoneMobile + ") " +
                        "  " + LocalisationService.getString("wantsToContact", lang) + "\n" +
                        LocalisationService.getString("PleaseRespond", lang) + "\n" +
                        "-------------------" + "\n" +
                        LocalisationService.getString("EventID", lang) + ": " + eventId + "\n" +
                        LocalisationService.getString("Category", lang) + ":  " + categoryName + "\n" +
                        LocalisationService.getString("EventTopic", lang) + ":  " + eventName + "\n" +
                        LocalisationService.getString("EventDescription", lang) + ":  " + eventDescription + "\n" + "\n" +
                        LocalisationService.getString("DateCreated", lang) + ":  " + regDate + "\n" +
                        LocalisationService.getString("DateStart", lang) + ":  " + startDate + "\n" +
                        LocalisationService.getString("DateEnd", lang) + ":  " + endDate + "\n" +
                        "---------------------" + "\n" +
                        LocalisationService.getString("Address", lang) + ": " + address + "\n"
                        + LocalisationService.getString("YouCanContactThis", lang);


                // Get user by telegram id
                lifeMapApi.getUserByExtId(extUserId)
                        .onComplete(res -> {
                            LOGGER.info(res);
                            JsonObject user = res.result().getJsonObject("result");

                            // Get user external id
                            if (user.containsKey("ext_user_id") && user.getLong("ext_user_id") != null) {
                                LOGGER.info(user.getLong("ext_user_id"));
                                int extUserIdForSend = user.getInteger("ext_user_id");

                                try {
                                    botWebClient.sendMessage(extUserIdForSend, aboutEventResponseMessage).onComplete(sendMessageHandler(context));
                                } catch (Exception e) {
                                    LOGGER.error("Error in eventResponsed: " + e.getMessage());
                                    e.printStackTrace();
                                }
                            }
                        })
                        .onFailure(res -> {
                            if (res.getCause().getMessage() != null) {
                                ResponseUtil.respondError(context, res.getCause().getMessage());
                                LOGGER.error("Request failed -> " + res.getCause().getMessage());
                            } else {
                                ResponseUtil.respondError(context, res.getCause().toString());
                                LOGGER.error("Request failed -> " + res.getCause());
                            }
                        });


            } else {
                ResponseUtil.respondError(context, "Not found some post data");
            }
        } else {
            ResponseUtil.respondError(context, "No data for fields");
        }
    }

    /**
     * Responder to client. Here we form result structure
     *
     * @param context Routing context that received
     * @return Response with headers
     */
    protected Handler<AsyncResult<JsonObject>> sendMessageHandler(RoutingContext context) {
        return message -> {
            if (message.succeeded()) {
                if (message.result() != null) {
                    ResponseUtil.respondWithCreated(context, message.result());
                } else {
                    ResponseUtil.respondError(context, "false");
                }
            } else {
                if (message.cause().getMessage() != null) {
                    LOGGER.error("Request (botWebClient.sendMessage) failed -> " + message.cause().getMessage());
                    ResponseUtil.respondError(context, message.cause().getMessage());
                } else {
                    LOGGER.error("Request (botWebClient.sendMessage) failed -> " + message.cause());
                    ResponseUtil.respondError(context, message.cause().toString());
                }
            }
        };
    }

    public Router getRouter() {
        return router;
    }


    private DatabaseManager dbManager() {
        return DatabaseManager.getInstance(this.config.getJsonObject("db"));
    }

    private String getUserLang(Integer telegramUserId) {
        final List<String> userOptions = dbManager().getUserOptions(telegramUserId);
        String language;

        if (userOptions.size() > 0 && userOptions.get(0) != null) {
            language = userOptions.get(0);
        } else {
            language = DEFAULT_LANGUAGE;
        }
        return language;
    }

}
