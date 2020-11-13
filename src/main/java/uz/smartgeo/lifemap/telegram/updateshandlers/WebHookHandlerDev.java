package uz.smartgeo.lifemap.telegram.updateshandlers;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.log4j.Logger;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import uz.smartgeo.lifemap.telegram.BotConfig;
import uz.smartgeo.lifemap.telegram.Commands;
import uz.smartgeo.lifemap.telegram.database.DatabaseManager;
import uz.smartgeo.lifemap.telegram.services.BotWebClientSync;
import uz.smartgeo.lifemap.telegram.services.LifeMapApiSync;
import uz.smartgeo.lifemap.telegram.services.LocalisationService;
import uz.smartgeo.lifemap.telegram.utils.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static uz.smartgeo.lifemap.telegram.utils.LIFEMAP_CONST.DEFAULT_LANGUAGE;

/**
 * Life map telegram bot handlers
 */
public class WebHookHandlerDev extends TelegramLongPollingBot {
    private static final String LOGTAG = "LIFEMAP_HANDLERS";


    private static final Logger LOGGER = Logger.getLogger(WebHookHandlerDev.class);

    protected BotWebClientSync botWebClientSync;
    protected LifeMapApiSync lifeMapApiSync;

    private final ConcurrentHashMap<Integer, Integer> userAddedCategory = new ConcurrentHashMap<>();

    JsonObject config;

    private static final int STATE_START = 0;
    private static final int STATE_DETECT_LANGUAGE = 1;
    private static final int STATE_MAIN_MENU = 2;

    private static final int STATE_EVENTS = 21;
    private static final int STATE_ADD_EVENT_CATEGORY = 22;
    private static final int STATE_ADD_EVENT_NAME = 23;
    private static final int STATE_ADD_EVENT_DESCRIPTION = 24;
    private static final int STATE_ADD_EVENT_LOCATION = 25;

    private static final int STATE_SETTINGS = 31;
    private static final int STATE_SETTINGS_LANGUAGE = 32;

    public WebHookHandlerDev(Vertx vertx, JsonObject config) {
        super();
        LOGGER.info("WebHookHandlers CONSTRUCTOR START");

        this.config = config;
        dbManager();

        createBotWebClientSync(config);

        createLifeMapApiWebClient(vertx);
    }

    private void createBotWebClientSync(JsonObject config) {
        botWebClientSync = new BotWebClientSync(config);
    }

    private void createLifeMapApiWebClient(Vertx vertx) {
        ConfigRetrieverOptions lmApiConfigRetrieverOptions = new ConfigRetrieverOptions()
                .addStore(new ConfigStoreOptions().setType("file").setConfig(
                        new JsonObject().put("path", "conf/lm-config.json")));

        ConfigRetriever configRetriever = ConfigRetriever.create(vertx, lmApiConfigRetrieverOptions);

        LOGGER.info("lm-config load start");

        configRetriever.getConfig(config -> {
            LOGGER.info("lm-config load success");

            JsonObject conf = config.result();

            String host = conf.getString("server.host_sync");
            Integer port = conf.getInteger("server.port");

            lifeMapApiSync = new LifeMapApiSync(host, port);
        });
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (update.hasCallbackQuery()) {
                handleIncomingCallbackQuery(update.getCallbackQuery());
                return;
            }

            if (update.hasMessage()) {
                Message message = update.getMessage();
                if (message.hasText() || message.hasLocation() || message.hasContact()) {
                    handleIncomingMessage(message);
                }
            }

            if (update.hasEditedMessage()) {
                Message editedMessage = update.getEditedMessage();
                if (editedMessage.hasLocation()) {
                    handleIncomingMessage(editedMessage);
                }
            }

        } catch (Exception e) {
            LOGGER.error(LOGTAG, e);
        }
    }

    // region Incoming messages handlers

    private void handleIncomingMessage(Message message) throws TelegramApiException {
        long chatId = message.getChatId();
        Integer telegramUserId = message.getFrom().getId();
        final String language;
        boolean hasOptions;

        int state = dbManager().getLifeMapBotState(telegramUserId, chatId);

        final List<String> userOptions = dbManager().getUserOptions(telegramUserId);

        if (userOptions.size() > 0 && userOptions.get(0) != null && userOptions.get(1) != null) {
            hasOptions = true;
            language = userOptions.get(0);
        } else {
            hasOptions = false;
            language = DEFAULT_LANGUAGE;
        }

        JsonObject dbUser;
        dbUser = lifeMapApiSync.getUserByExtIdSync(telegramUserId);

        if (message.hasText()) {
            if (isCommandForOther(message.getText())) {
                return;
            } else if (message.getText().startsWith(Commands.ADD_EVENT_COMMAND)) {
                sendAddEventCategoryCommand(message, language);
                return;
            } else if (message.getText().startsWith(Commands.SET_LANGUAGE_COMMAND)) {
                sendSetLanguageCommand(message, language);
                return;
            } else if (message.getText().startsWith(Commands.SUPPORT_COMMAND)) {
                sendSupportCommand(message, language);
                return;
            } else if (message.getText().startsWith(Commands.STOPCOMMAND)) {
                sendHideKeyboard(telegramUserId, chatId, message.getMessageId());
                return;
            }
        }

        if (state == 0) {
            state = detectStateOnStart(hasOptions);
        }

        stateProcessing(state, message, language, dbUser);
    }

    /**
     * For an CallbackQuery
     *
     * @param callbackQuery CallbackQuery received
     */
    private void handleIncomingCallbackQuery(CallbackQuery callbackQuery) {
        String dataValue = callbackQuery.getData();
        LOGGER.info(dataValue);

        try {
            if (!dataValue.isEmpty()) {
                Long eventId = Long.parseLong(dataValue.substring(0, dataValue.lastIndexOf("=")));
                Boolean moderationStatus = Boolean.parseBoolean(dataValue.substring(dataValue.lastIndexOf("=") + 1));

                System.out.println(eventId);
                System.out.println(moderationStatus);

                if (eventId != null && moderationStatus != null) {
                    SendMessage sendMessageRequest;
                    boolean saveComplete;
                    final String language;
                    Integer telegramUserId = callbackQuery.getFrom().getId();
                    final List<String> userOptions = dbManager().getUserOptions(telegramUserId);

                    Message message = callbackQuery.getMessage();

                    if (userOptions.size() > 0 && userOptions.get(0) != null && userOptions.get(1) != null) {
                        language = userOptions.get(0);
                    } else {
                        language = DEFAULT_LANGUAGE;
                    }

                    saveComplete = lifeMapApiSync.updateModerationStatusSync(eventId, moderationStatus);

                    if (saveComplete) {
                        sendMessageRequest = sendUpdateModerationStatusMessage(message.getChatId(), message.getMessageId(),
                                getMainMenuKeyboard(language, true), language);
                    } else {
                        sendMessageRequest = sendUpdateModerationStatusFailedMessage(message.getChatId(), message.getMessageId(),
                                getMainMenuKeyboard(language, false), language);
                    }
                    execute(sendMessageRequest);
                }

//                    InlineKeyboardMarkup markup = null;
//
//                    String messageText = callbackQuery.getMessage().getText();
//
//                    EditMessageText editMarkup = new EditMessageText();
//                    editMarkup.setChatId(callbackQuery.getMessage().getChatId().toString());
//                    editMarkup.setInlineMessageId(callbackQuery.getInlineMessageId());
//                    editMarkup.setText(messageText.substring(0,  messageText.lastIndexOf(" lifemap.uz") + 1) );
//                    editMarkup.enableMarkdown(true);
//                    editMarkup.setMessageId(callbackQuery.getMessage().getMessageId());
//                    editMarkup.setReplyMarkup(markup);
//                    try {
//                        execute(editMarkup);
//                    } catch (TelegramApiException e) {
//                        e.printStackTrace();
//                    }
//                }
            }
        } catch (Exception e) {
            LOGGER.error(e);
        }
    }

    private int detectStateOnStart(boolean hasOptions) {
        int state;

        if (!hasOptions) {
            state = STATE_DETECT_LANGUAGE;
        } else {
            state = STATE_MAIN_MENU;
        }

        return state;
    }
//
//    private int detectStateOnMainMenu(boolean hasContact, boolean dbUserHasMobileNumber) {
//        int state = 2;
//
////        if (!hasOptions) {
////            state = STATE_DETECT_LANGUAGE;
////        } else {
////            state = STATE_MAIN_MENU;
////        }
//
//        return state;
//    }

    private void stateProcessing(Integer state, Message message, String language,
                                 JsonObject dbUser) throws TelegramApiException {
        SendMessage sendMessageRequest;

        String dbUserMobileNumber;
        Long dbUserId = 0L;

        boolean dbUserHasMobileNumber = false;
        String dbUserFirstName = "";

        if (dbUser != null) {
            dbUserMobileNumber = dbUser.getString("phone_mobile");

            dbUserId = dbUser.getLong("id");
            dbUserFirstName = dbUser.getString("first_name");

            if (dbUserMobileNumber != null && !dbUserMobileNumber.isEmpty() && dbUserMobileNumber.length() >= 8) {
                dbUserHasMobileNumber = true;
            }
        }

        switch (state) {
            case STATE_DETECT_LANGUAGE:
                sendMessageRequest = messageOnStartLanguageDetect(message, dbUserFirstName, dbUserHasMobileNumber);
                break;
            case STATE_MAIN_MENU:
                sendMessageRequest = messageOnMainMenu(message, language, dbUser, dbUserHasMobileNumber);
                break;
            case STATE_EVENTS:
                sendMessageRequest = messageOnEvents(message, language, dbUserHasMobileNumber);
                break;
            case STATE_ADD_EVENT_CATEGORY:
                sendMessageRequest = messageOnAddEventCategory(message, language, dbUserId);
                break;
            case STATE_ADD_EVENT_NAME:
                sendMessageRequest = messageOnAddEventName(message, language, dbUserId);
                break;
            case STATE_ADD_EVENT_DESCRIPTION:
                sendMessageRequest = messageOnAddEventDescription(message, language, dbUserId);
                break;
            case STATE_ADD_EVENT_LOCATION:
                sendMessageRequest = messageOnAddEventLocation(message, language, dbUserId, dbUser);
                break;
            case STATE_SETTINGS:
                sendMessageRequest = messageOnSetting(message, language, dbUserHasMobileNumber);
                break;
            case STATE_SETTINGS_LANGUAGE:
                sendMessageRequest = messageOnLanguage(message, language, dbUserHasMobileNumber);
                break;
            default:
                sendMessageRequest = sendMessageDefault(message, language, dbUserHasMobileNumber);
                break;
        }

        execute(sendMessageRequest);
    }

    private void sendAddEventCategoryCommand(Message message, String language) throws TelegramApiException {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);

        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.setChatId(message.getChatId());

        sendMessage.setReplyMarkup(getAddEventCategoryKeyboard(language));
        sendMessage.setText(eventCategoryMessage(language));
        sendMessage.disableWebPagePreview();

        execute(sendMessage);
        dbManager().insertLifeMapBotState(message.getFrom().getId(), message.getChatId(), STATE_ADD_EVENT_CATEGORY);
    }

    private void sendSetLanguageCommand(Message message, String language) throws TelegramApiException {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);

        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.setChatId(message.getChatId());
        sendMessage.setReplyMarkup(getLanguagesKeyboard(language, false));
        sendMessage.setText(getLanguageMessage(language));

        sendMessage.disableWebPagePreview();

        execute(sendMessage);
        dbManager().insertLifeMapBotState(message.getFrom().getId(), message.getChatId(), STATE_SETTINGS_LANGUAGE);
    }

    private void sendSupportCommand(Message message, String language) throws TelegramApiException {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(message.getChatId());
        sendMessage.setReplyToMessageId(message.getMessageId());

        sendMessage.setText(LocalisationService.getString("supportMessage", language));

        sendMessage.disableWebPagePreview();

        execute(sendMessage);
    }

    private void sendHideKeyboard(Integer userId, Long chatId, Integer messageId) throws TelegramApiException {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId.toString());
        sendMessage.enableMarkdown(true);
        sendMessage.setReplyToMessageId(messageId);
        sendMessage.setText(Emoji.WAVING_HAND_SIGN.toString());

        ReplyKeyboardRemove replyKeyboardRemove = new ReplyKeyboardRemove();
        replyKeyboardRemove.setSelective(true);
        sendMessage.setReplyMarkup(replyKeyboardRemove);

        execute(sendMessage);
        dbManager().insertLifeMapBotState(userId, chatId, STATE_START);
    }

    private static boolean isCommandForOther(String text) {
        boolean isSimpleCommand = text.equals("/start") || text.equals("/help") || text.equals("/stop");

        boolean isCommandForMe =
                text.equals("/addevent") ||
                        text.equals("/setlanguage") ||
                        text.equals("/support") ||
                        text.equals("/help@tg_test_2020_bot") ||
                        text.equals("/stop@tg_test_2020_bot");

        return text.startsWith("/") && !isSimpleCommand && !isCommandForMe;
    }

    // endregion Incoming messages handlers

    // region start language detect selected

    private SendMessage messageOnStartLanguageDetect(Message message, String dbUserFirstName, boolean dbUserHasMobileNumber) {
        SendMessage sendMessageRequest = null;

        if (message.hasText()) {
            if (message.getText().startsWith(Commands.startCommand)) {
                sendMessageRequest = onStartLanguageDetectCommand(message, dbUserFirstName);
            } else if (LocalisationService.getLanguageByName(message.getText().trim()) != null) {
                sendMessageRequest = onStartLanguageChosen(
                        message.getFrom().getId(),
                        message.getChatId(),
                        message.getMessageId(),
                        message.getText().trim(),
                        dbUserHasMobileNumber
                );
            } else {
                sendMessageRequest = onStartLanguageDetectCommand(message, dbUserFirstName);
            }
        }
        return sendMessageRequest;
    }

    private SendMessage onStartLanguageDetectCommand(Message message, String dbUserFirstName) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);

        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.setChatId(message.getChatId());
        sendMessage.setReplyMarkup(getLanguagesKeyboard(DEFAULT_LANGUAGE, true));

        sendMessage.setText(getLanguageDetectStartMessage(dbUserFirstName));

        dbManager().insertLifeMapBotState(message.getFrom().getId(), message.getChatId(), STATE_DETECT_LANGUAGE);
        return sendMessage;
    }
    // endregion start language detect

    // region Main menu options selected

    private SendMessage messageOnMainMenu(Message message, String language, JsonObject dbUser, boolean dbUserHasMobileNumber) {
        SendMessage sendMessageRequest;

        if (message.hasText()) {
            if (message.getText().equals(Commands.startCommand)) {
                sendMessageRequest = sendMessageDefault(message, language, dbUserHasMobileNumber);
            } else if (message.getText().equals(getEventsCommand(language))) {
                sendMessageRequest = onEventsClick(message, language);
            } else if (message.getText().equals(getSettingsCommand(language))) {
                sendMessageRequest = onSettingsClick(message, language);
            } else if (message.getText().equals(getSupportCommand(language))) {
                sendMessageRequest = sendSupportMessage(message.getChatId(), message.getMessageId(), language);
            } else {
                sendMessageRequest = sendChooseOptionMessage(message.getChatId(), message.getMessageId(),
                        getMainMenuKeyboard(language, dbUserHasMobileNumber), language);
            }
        } else if (!message.hasText() && message.hasContact()) {
            sendMessageRequest = saveContact(message, dbUser, language);
        } else {
            sendMessageRequest = sendChooseOptionMessage(message.getChatId(), message.getMessageId(),
                    getMainMenuKeyboard(language, dbUserHasMobileNumber), language);
        }

        return sendMessageRequest;
    }

    private SendMessage onEventsClick(Message message, String language) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);

        ReplyKeyboardMarkup replyKeyboardMarkup = getEventsKeyboard(language);
        sendMessage.setReplyMarkup(replyKeyboardMarkup);
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.setChatId(message.getChatId());
        sendMessage.setText(getEventsMessage(language));

        sendMessage.disableWebPagePreview();

        dbManager().insertLifeMapBotState(message.getFrom().getId(), message.getChatId(), STATE_EVENTS);
        return sendMessage;
    }

    private SendMessage onSettingsClick(Message message, String language) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);

        ReplyKeyboardMarkup replyKeyboardMarkup = getSettingsKeyboard(language);
        sendMessage.setReplyMarkup(replyKeyboardMarkup);
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.setChatId(message.getChatId());
        sendMessage.setText(getSettingsMessage(language));

        sendMessage.disableWebPagePreview();

        dbManager().insertLifeMapBotState(message.getFrom().getId(), message.getChatId(), STATE_SETTINGS);
        return sendMessage;
    }

    // endregion Main menu options selected

    // region Events Menu Option selected

    private SendMessage messageOnEvents(Message message, String language, boolean dbUserHasMobileNumber) {
        SendMessage sendMessageRequest;
        if (message.hasText()) {
            if (message.getText().startsWith(getAddEventCategoryCommand(language))) {
                sendMessageRequest = onAddEventCategoryCommand(message, language);
            } else if (message.getText().startsWith(getBackCommand(language))) {
                sendMessageRequest = sendMessageDefault(message, language, dbUserHasMobileNumber);
            } else {
                sendMessageRequest = sendChooseOptionMessage(message.getChatId(), message.getMessageId(),
                        getEventsKeyboard(language), language);
            }
        } else {
            sendMessageRequest = sendChooseOptionMessage(message.getChatId(), message.getMessageId(),
                    getEventsKeyboard(language), language);
        }
        return sendMessageRequest;
    }

    private SendMessage onAddEventCategoryCommand(Message message, String language) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);

        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.setChatId(message.getChatId());

        sendMessage.setReplyMarkup(getAddEventCategoryKeyboard(language));
        sendMessage.setText(eventCategoryMessage(language));
        sendMessage.disableWebPagePreview();
        dbManager().insertLifeMapBotState(message.getFrom().getId(), message.getChatId(), STATE_ADD_EVENT_CATEGORY);
        return sendMessage;
    }

    private SendMessage messageOnAddEventCategory(Message message, String language,
                                                  long dbUserId) {
        SendMessage sendMessageRequest = null;
        if (message.hasText()) {
            if (LocalisationService.getCategoryByName(message.getText().trim(), language) != null) {
                sendMessageRequest = onAddEventCategoryChosen(
                        message.getFrom().getId(),
                        dbUserId,
                        message.getChatId(),
                        message.getMessageId(),
                        message.getText().trim(),
                        language
                );
            } else if (message.getText().trim().equals(getBackCommand(language))) {
                sendMessageRequest = onBackAddEventCategoryCommand(message, language);
            } else {
                sendMessageRequest = onAddEventCategoryError(message.getChatId(), message.getMessageId(), language);
                dbManager().insertLifeMapBotState(message.getFrom().getId(), message.getChatId(), STATE_EVENTS);
            }
        }
        return sendMessageRequest;
    }

    private SendMessage messageOnAddEventName(Message message, String language,
                                              long dbUserId) {
        SendMessage sendMessageRequest = null;
        if (message.hasText()) {
            if (message.getText().trim().equals(getBackCommand(language))) {
                sendMessageRequest = onBackAddEventNameCommand(message, language);
            } else {
                sendMessageRequest = onAddEventNameSave(
                        message.getFrom().getId(),
                        dbUserId,
                        message.getChatId(),
                        message.getMessageId(),
                        language,
                        message.getText().trim());

                dbManager().insertLifeMapBotState(message.getFrom().getId(), message.getChatId(), STATE_ADD_EVENT_DESCRIPTION);
            }
        }
        return sendMessageRequest;
    }

    private SendMessage messageOnAddEventDescription(Message message, String language,
                                                     Long dbUserId) {
        SendMessage sendMessageRequest = null;

        if (message.hasText()) {
            if (message.getText().trim().equals(getBackCommand(language))) {
                sendMessageRequest = onBackAddEventDescriptionCommand(message, language);
            } else {
                sendMessageRequest = onAddEventDescriptionSave(
                        message.getFrom().getId(),
                        dbUserId,
                        message.getChatId(),
                        message.getMessageId(),
                        language,
                        message.getText().trim());

                dbManager().insertLifeMapBotState(message.getFrom().getId(), message.getChatId(), STATE_ADD_EVENT_LOCATION);
            }
        }

        return sendMessageRequest;
    }

    private SendMessage messageOnAddEventLocation(Message message, String language,
                                                  Long dbUserId, JsonObject dbUser) {
        SendMessage sendMessageRequest = null;

        if (message.hasLocation()) {
            sendMessageRequest = onAddEventLocationSave(
                    message.getFrom().getId(),
                    dbUserId,
                    message.getChatId(),
                    message.getMessageId(),
                    language,
                    message.getLocation(),
                    dbUser);
            if (sendMessageRequest.getText().equals(eventSavedMessage(language))) {
                dbManager().insertLifeMapBotState(message.getFrom().getId(), message.getChatId(), STATE_EVENTS);
            } else {
                LOGGER.error(sendMessageRequest.getText());
            }
        } else if (message.hasText()) {
            if (message.getText().trim().equals(getBackCommand(language))) {
                sendMessageRequest = onBackAddEventLocationCommand(message, language);
            } else {
                sendMessageRequest = onAddEventLocationFailed(
                        message.getChatId(),
                        message.getMessageId(),
                        language);

                dbManager().insertLifeMapBotState(message.getFrom().getId(), message.getChatId(), STATE_ADD_EVENT_LOCATION);
            }
        }
        return sendMessageRequest;
    }

    private SendMessage onAddEventCategoryChosen(Integer telegramUserId, Long dbUserId, Long chatId,
                                                 Integer messageId, String categoryName,
                                                 String language) {

        userAddedCategory.remove(telegramUserId);

        int selectedCategoryId = LocalisationService.getCategoryByName(categoryName, language).getCode();
        dbManager().addBotEvent(telegramUserId, dbUserId, selectedCategoryId);

        SendMessage sendMessageRequest = new SendMessage();
        sendMessageRequest.enableMarkdown(true);
        sendMessageRequest.setChatId(chatId.toString());

        sendMessageRequest.setText(eventNameMessage(language));

        sendMessageRequest.setReplyToMessageId(messageId);
        sendMessageRequest.setReplyMarkup(getAddEventNameKeyboard(language));

        sendMessageRequest.disableWebPagePreview();

        userAddedCategory.put(telegramUserId, selectedCategoryId);

        dbManager().insertLifeMapBotState(telegramUserId, chatId, STATE_ADD_EVENT_NAME);
        return sendMessageRequest;
    }

    private SendMessage onAddEventNameSave(Integer telegramUserId, Long dbUserId, Long chatId, Integer messageId, String language,
                                           String eventName) {
        dbManager().updateBotEventName(telegramUserId, dbUserId, eventName);

        SendMessage sendMessageRequest = new SendMessage();
        sendMessageRequest.enableMarkdown(true);
        sendMessageRequest.setChatId(chatId.toString());

        sendMessageRequest.setText(eventDescriptionMessage(language));

        sendMessageRequest.setReplyToMessageId(messageId);
        sendMessageRequest.setReplyMarkup(getAddEventDescriptionKeyboard(language));

        sendMessageRequest.disableWebPagePreview();

        dbManager().insertLifeMapBotState(telegramUserId, chatId, STATE_ADD_EVENT_DESCRIPTION);
        return sendMessageRequest;
    }

    private SendMessage onAddEventDescriptionSave(Integer telegramUserId, Long dbUserId, Long chatId, Integer messageId, String language,
                                                  String eventName) {

        dbManager().updateBotEventDescription(telegramUserId, dbUserId, eventName);

        SendMessage sendMessageRequest = new SendMessage();
        sendMessageRequest.enableMarkdown(true);
        sendMessageRequest.setChatId(chatId.toString());

        sendMessageRequest.setText(eventLocationMessage(language));

        sendMessageRequest.setReplyToMessageId(messageId);
        sendMessageRequest.setReplyMarkup(getAddEventLocationKeyboard(language));

        sendMessageRequest.disableWebPagePreview();

        dbManager().insertLifeMapBotState(telegramUserId, chatId, STATE_ADD_EVENT_LOCATION);
        return sendMessageRequest;
    }

    private SendMessage onAddEventLocationSave(Integer telegramUserId, Long dbUserId,
                                               Long chatId, Integer messageId,
                                               String language,
                                               Location eventLocation, JsonObject dbUser) {

        double lon = eventLocation.getLongitude();
        double lat = eventLocation.getLatitude();

        JsonObject eventGeoJson = new JsonObject();
        JsonObject geometryObject = new JsonObject();

        JsonArray coordinates = new JsonArray();
        coordinates.add(lon);
        coordinates.add(lat);

        geometryObject.put("type", "Point");
        geometryObject.put("coordinates", coordinates);

        eventGeoJson.put("type", "Feature");
        eventGeoJson.put("geometry", geometryObject);
        eventGeoJson.put("id", -1999);

        SendMessage sendMessageRequest = new SendMessage();
        sendMessageRequest.enableMarkdown(true);
        sendMessageRequest.setChatId(chatId.toString());
        sendMessageRequest.disableWebPagePreview();
        sendMessageRequest.setReplyToMessageId(messageId);

//        System.out.println(eventGeoJson.encodePrettily());
//        System.out.println(eventGeoJson.toString());
//        System.out.println(eventGeoJson.encode());

        JsonObject addressRes = botWebClientSync.getAddressByCoordinates(lon, lat, language);

        if (addressRes != null) {
            JsonObject address = addressRes.getJsonObject("address");

            JsonObject addressGeoJson = AddressUtils.getAddressFormatted(addressRes);
            String countryCode = AddressUtils.getAddressCountryCode(address);
            String region = AddressUtils.getAddressRegion(address);

            int selectedCategoryId = userAddedCategory.getOrDefault(telegramUserId, 1);

            String startDate = DateUtils.getCurrentISOStringUTC();
            String endDate = EventUtils.getEventEndDateByCategory(selectedCategoryId);

            Long addedBotEventId = dbManager().updateBotEventGeoJson(
                    telegramUserId,
                    dbUserId,
                    eventGeoJson.encode(),
                    addressGeoJson.encode(),
                    startDate,
                    endDate,
                    countryCode,
                    region);

            if (addedBotEventId > 0) {
                Long eventRegionId = dbManager().getRegionIdByCoordinates(lon, lat);

                boolean importEventSuccess = dbManager().importTrack(addedBotEventId, eventRegionId);

                if (importEventSuccess) {
                    sendMessageRequest.setText(eventSavedMessage(language));
                    sendMessageRequest.setReplyMarkup(getEventsKeyboard(language));

                    dbManager().updateBotEventImportValues(addedBotEventId);

                    JsonObject event = dbManager().getBotEvent(addedBotEventId);

                    dbManager().insertLifeMapBotState(telegramUserId, chatId, STATE_EVENTS);

                    String eventStartDate = event.getString("start_date");
                    String eventEndDate = event.getString("end_date");
                    String eventRegDate = event.getString("reg_date");

                    try {
                        eventStartDate = DateUtils.getISOString(eventStartDate);
                        eventEndDate = DateUtils.getISOString(eventEndDate);
                        eventRegDate = DateUtils.getISOString(eventRegDate);


                        botWebClientSync.notifyAboutNewEvent(telegramUserId, dbUserId, addedBotEventId,
                                selectedCategoryId,
                                event.getString("name"),
                                event.getString("description"),
                                event.getString("address"),
                                eventStartDate,
                                eventEndDate,
                                eventRegDate,
                                dbUser.getString("first_name"),
                                dbUser.getString("last_name"),
                                dbUser.getString("login"),
                                eventRegionId
                        );
                    } catch (Exception e) {
                        LOGGER.error("Date parse error when notify about new event from tg bot ");
                        e.printStackTrace();
                        sendMessageRequest.setText(eventSaveMessageFailed(language));
                        sendMessageRequest.setReplyMarkup(getEventsKeyboard(language));

                        dbManager().insertLifeMapBotState(telegramUserId, chatId, STATE_EVENTS);
                    }

                } else {
                    sendMessageRequest.setText(eventSaveMessageFailed(language));
                    sendMessageRequest.setReplyMarkup(getAddEventLocationKeyboard(language));

                    dbManager().insertLifeMapBotState(telegramUserId, chatId, STATE_ADD_EVENT_LOCATION);
                }

            } else {
                sendMessageRequest.setText(eventSaveMessageFailed(language));
                sendMessageRequest.setReplyMarkup(getEventsKeyboard(language));

                dbManager().insertLifeMapBotState(telegramUserId, chatId, STATE_EVENTS);
            }
        } else {
            sendMessageRequest.setText(eventSaveMessageFailed(language));
            sendMessageRequest.setReplyMarkup(getAddEventLocationKeyboard(language));

            dbManager().insertLifeMapBotState(telegramUserId, chatId, STATE_ADD_EVENT_LOCATION);
        }

        return sendMessageRequest;
    }

    private SendMessage onAddEventLocationFailed(Long chatId, Integer messageId, String language) {
        SendMessage sendMessageRequest = new SendMessage();
        sendMessageRequest.enableMarkdown(true);
        sendMessageRequest.setChatId(chatId.toString());
        sendMessageRequest.disableWebPagePreview();
        sendMessageRequest.setReplyToMessageId(messageId);

        LOGGER.error("Location not found");

        sendMessageRequest.setText(eventSaveMessageLocationFailed(language));
        sendMessageRequest.setReplyMarkup(getAddEventLocationKeyboard(language));

        return sendMessageRequest;
    }

    private SendMessage saveContact(Message message, JsonObject dbUser, String language) {
        SendMessage sendMessageRequest = new SendMessage();

        try {
            boolean saveComplete;
            Contact contact = message.getContact();

            if (dbUser == null) {
                String login = "";

                if (message.getChat().getUserName() != null) {
                    login = message.getChat().getUserName();
                }

                String firstName = contact.getFirstName();
                String lastName = contact.getLastName();
                String phoneNumber = contact.getPhoneNumber().replaceFirst("\\+", "");

                saveComplete = dbManager().saveUser(message.getFrom().getId(), login, firstName, lastName, phoneNumber);
            } else {
                saveComplete = lifeMapApiSync.updateUserMobileNumberSync(message.getFrom().getId(), message.getContact().getPhoneNumber());
            }

            if (saveComplete) {
                sendMessageRequest = sendShareContactMessage(message.getChatId(), message.getMessageId(),
                        getMainMenuKeyboard(language, true), language);
            } else {
                sendMessageRequest = sendShareContactFailedMessage(message.getChatId(), message.getMessageId(),
                        getMainMenuKeyboard(language, false), language);
            }
        } catch (Exception e) {
            LOGGER.error(e);
        }

        return sendMessageRequest;
    }
    // endregion Events Menu Option selected

    // region Settings Menu Option selected

    private SendMessage messageOnSetting(Message message, String language, boolean dbUserHasMobileNumber) {
        SendMessage sendMessageRequest = null;
        if (message.hasText()) {
            if (message.getText().startsWith(getLanguagesCommand(language))) {
                sendMessageRequest = onLanguageCommand(message, language);
            } else if (message.getText().startsWith(getBackCommand(language))) {
                sendMessageRequest = sendMessageDefault(message, language, dbUserHasMobileNumber);
            } else {
                sendMessageRequest = sendChooseOptionMessage(message.getChatId(), message.getMessageId(),
                        getSettingsKeyboard(language), language);
            }
        }
        return sendMessageRequest;
    }

    private SendMessage onLanguageCommand(Message message, String language) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);

        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.setChatId(message.getChatId());
        sendMessage.setReplyMarkup(getLanguagesKeyboard(language, false));
        sendMessage.setText(getLanguageMessage(language));

        sendMessage.disableWebPagePreview();

        dbManager().insertLifeMapBotState(message.getFrom().getId(), message.getChatId(), STATE_SETTINGS_LANGUAGE);
        return sendMessage;
    }

    // endregion Settings Menu Option selected

    // region Language Menu Option selected

    private SendMessage messageOnLanguage(Message message, String language, boolean dbUserHasMobileNumber) {
        SendMessage sendMessageRequest = null;
        if (message.hasText()) {
            if (message.getText().trim().equals(getCancelCommand(language))) {
                sendMessageRequest = onBackLanguageCommand(message, language);
            } else if (LocalisationService.getLanguageByName(message.getText().trim()) != null) {
                sendMessageRequest = onLanguageChosen(
                        message.getFrom().getId(),
                        message.getChatId(),
                        message.getMessageId(),
                        message.getText().trim(),
                        dbUserHasMobileNumber
                );
            } else {
                sendMessageRequest = onLanguageError(message.getChatId(), message.getMessageId(), language);
            }
        }
        return sendMessageRequest;
    }

    private SendMessage onBackAddEventCategoryCommand(Message message, String language) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);

        ReplyKeyboardMarkup replyKeyboardMarkup = getEventsKeyboard(language);
        sendMessage.setReplyMarkup(replyKeyboardMarkup);
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.setChatId(message.getChatId());
        sendMessage.setText(getEventsMessage(language));

        sendMessage.disableWebPagePreview();

        dbManager().insertLifeMapBotState(message.getFrom().getId(), message.getChatId(), STATE_EVENTS);
        return sendMessage;
    }

    private SendMessage onBackAddEventNameCommand(Message message, String language) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);

        ReplyKeyboardMarkup replyKeyboardMarkup = getAddEventCategoryKeyboard(language);
        sendMessage.setReplyMarkup(replyKeyboardMarkup);
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.setChatId(message.getChatId());
        sendMessage.setText(eventCategoryMessage(language));

        sendMessage.disableWebPagePreview();

        dbManager().insertLifeMapBotState(message.getFrom().getId(), message.getChatId(), STATE_ADD_EVENT_CATEGORY);
        return sendMessage;
    }

    private SendMessage onBackAddEventDescriptionCommand(Message message, String language) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);

        ReplyKeyboardMarkup replyKeyboardMarkup = getAddEventNameKeyboard(language);
        sendMessage.setReplyMarkup(replyKeyboardMarkup);
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.setChatId(message.getChatId());
        sendMessage.setText(eventNameMessage(language));

        sendMessage.disableWebPagePreview();

        dbManager().insertLifeMapBotState(message.getFrom().getId(), message.getChatId(), STATE_ADD_EVENT_NAME);
        return sendMessage;
    }

    private SendMessage onBackAddEventLocationCommand(Message message, String language) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);

        ReplyKeyboardMarkup replyKeyboardMarkup = getAddEventDescriptionKeyboard(language);
        sendMessage.setReplyMarkup(replyKeyboardMarkup);
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.setChatId(message.getChatId());
        sendMessage.setText(eventDescriptionMessage(language));

        sendMessage.disableWebPagePreview();

        dbManager().insertLifeMapBotState(message.getFrom().getId(), message.getChatId(), STATE_ADD_EVENT_DESCRIPTION);
        return sendMessage;
    }

    private SendMessage onBackLanguageCommand(Message message, String language) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);

        ReplyKeyboardMarkup replyKeyboardMarkup = getSettingsKeyboard(language);
        sendMessage.setReplyMarkup(replyKeyboardMarkup);
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.setChatId(message.getChatId());
        sendMessage.setText(getSettingsMessage(language));

        sendMessage.disableWebPagePreview();

        dbManager().insertLifeMapBotState(message.getFrom().getId(), message.getChatId(), STATE_SETTINGS);
        return sendMessage;
    }

    private SendMessage onAddEventCategoryError(Long chatId, Integer messageId, String language) {
        SendMessage sendMessageRequest = new SendMessage();
        sendMessageRequest.enableMarkdown(true);
        sendMessageRequest.setChatId(chatId.toString());
        sendMessageRequest.setReplyMarkup(getEventsKeyboard(language));
        sendMessageRequest.setText(LocalisationService.getString("addingEventCategoryFailed", language));
        sendMessageRequest.setReplyToMessageId(messageId);

        sendMessageRequest.disableWebPagePreview();

        return sendMessageRequest;
    }
//
//    private SendMessage onAddEventNameError(Long chatId, Integer messageId, String language) {
//        SendMessage sendMessageRequest = new SendMessage();
//        sendMessageRequest.enableMarkdown(true);
//        sendMessageRequest.setChatId(chatId.toString());
//        sendMessageRequest.setReplyMarkup(getEventsKeyboard(language));
//        sendMessageRequest.setText(LocalisationService.getString("addingEventNameFailed", language));
//        sendMessageRequest.setReplyToMessageId(messageId);
//
//        sendMessageRequest.disableWebPagePreview();
//
//        return sendMessageRequest;
//    }

    private SendMessage onLanguageError(Long chatId, Integer messageId, String language) {
        SendMessage sendMessageRequest = new SendMessage();
        sendMessageRequest.enableMarkdown(true);
        sendMessageRequest.setChatId(chatId.toString());
        sendMessageRequest.setReplyMarkup(getLanguagesKeyboard(language, false));
        sendMessageRequest.setText(LocalisationService.getString("errorLanguageNotFound", language));
        sendMessageRequest.setReplyToMessageId(messageId);

        sendMessageRequest.disableWebPagePreview();

        return sendMessageRequest;
    }

    private SendMessage onStartLanguageChosen(Integer userId, Long chatId, Integer messageId,
                                              String languageFullText, boolean dbUserHasMobileNumber) {

        String language = LocalisationService.getLanguageCodeByName(languageFullText);

        dbManager().putUserLanguageOption(userId, language);

        SendMessage sendMessageRequest = new SendMessage();
        sendMessageRequest.enableMarkdown(true);
        sendMessageRequest.setChatId(chatId.toString());

        sendMessageRequest.setText(getHelpMessage(language, dbUserHasMobileNumber));

        sendMessageRequest.setReplyToMessageId(messageId);
        sendMessageRequest.setReplyMarkup(getMainMenuKeyboard(language, dbUserHasMobileNumber));

        sendMessageRequest.disableWebPagePreview();

        dbManager().insertLifeMapBotState(userId, chatId, STATE_MAIN_MENU);
        return sendMessageRequest;
    }

    private SendMessage onLanguageChosen(Integer userId, Long chatId, Integer messageId,
                                         String language, boolean dbUserHasMobileNumber) {
        String languageCode = LocalisationService.getLanguageCodeByName(language);
        dbManager().putUserLanguageOption(userId, languageCode);

        SendMessage sendMessageRequest = new SendMessage();
        sendMessageRequest.enableMarkdown(true);
        sendMessageRequest.setChatId(chatId.toString());
        sendMessageRequest.setText(LocalisationService.getString("languageUpdated", languageCode));
        sendMessageRequest.setReplyToMessageId(messageId);
        sendMessageRequest.setReplyMarkup(getMainMenuKeyboard(languageCode, dbUserHasMobileNumber));

        sendMessageRequest.disableWebPagePreview();

        dbManager().insertLifeMapBotState(userId, chatId, STATE_MAIN_MENU);
        return sendMessageRequest;
    }

    // endregion Language Menu Option selected

    // region Get Messages

    private static String getEventsMessage(String language) {
        String baseString = LocalisationService.getString("onEventsCommand", language);

        return String.format(baseString, Emoji.PENCIL.toString(),
                Emoji.BACK_WITH_LEFTWARDS_ARROW_ABOVE.toString());
    }

    private static String getSettingsMessage(String language) {
        String baseString = LocalisationService.getString("onSettingsCommand", language);
        return String.format(baseString, Emoji.GLOBE_WITH_MERIDIANS.toString(),
                Emoji.BACK_WITH_LEFTWARDS_ARROW_ABOVE.toString());
    }

    private static String getHelpMessage(String language, boolean dbUserHasMobileNumber) {

        if (dbUserHasMobileNumber) {
            String baseString = LocalisationService.getString("helpLifeMapMessage", language);
            return String.format(baseString,
                    Emoji.EVENTS.toString(), Emoji.BLACK_RIGHT_POINTING_DOUBLE_TRIANGLE.toString(), Emoji.PENCIL.toString(),
                    Emoji.WRENCH.toString(), Emoji.BLACK_RIGHT_POINTING_DOUBLE_TRIANGLE.toString(), Emoji.GLOBE_WITH_MERIDIANS.toString(),
                    Emoji.BLACK_QUESTION_MARK_ORNAMENT.toString());
        } else {
            String baseString = LocalisationService.getString("helpLifeMapMessageWithShareContact", language);
            return String.format(baseString, Emoji.MOBILE_PHONE.toString(),
                    Emoji.WRENCH.toString(), Emoji.BLACK_RIGHT_POINTING_DOUBLE_TRIANGLE.toString(), Emoji.GLOBE_WITH_MERIDIANS.toString(),
                    Emoji.BLACK_QUESTION_MARK_ORNAMENT.toString());
        }
    }

    private static String getLanguageDetectStartMessage(String dbUserFirstName) {
        String baseString;

        if (dbUserFirstName.isEmpty()) {
            baseString = LocalisationService.getString("chooseLanguageOnStart");
        } else {
            baseString = LocalisationService.getString("chooseLanguageOnStartWithName", dbUserFirstName);
        }


        return String.format(baseString, dbUserFirstName);

    }

    private static String eventCategoryMessage(String language) {
        String baseString = String.format(LocalisationService.getString("eventCategoryMessage", language),
                Emoji.PIN.toString());
        return String.format(baseString, language);
    }

    private static String eventNameMessage(String language) {
        String baseString = String.format(LocalisationService.getString("eventNameMessage", language),
                Emoji.SUCCESS.toString());
        return String.format(baseString, language);
    }

    private static String eventDescriptionMessage(String language) {
        String baseString = String.format(LocalisationService.getString("eventDescriptionMessage", language),
                Emoji.SUCCESS.toString(),
                Emoji.SUCCESS.toString());
        return String.format(baseString, language);
    }

    private static String eventLocationMessage(String language) {
        String baseString = String.format(LocalisationService.getString("eventLocationMessage", language),
                Emoji.SUCCESS.toString(),
                Emoji.SUCCESS.toString(),
                Emoji.SUCCESS.toString(),
                Emoji.PIN.toString());
        return String.format(baseString, language);
    }

    private static String eventSavedMessage(String language) {
        String baseString = String.format(LocalisationService.getString("eventSavedMessage", language),
                Emoji.SUCCESS.toString());
        return String.format(baseString, language);
    }

    private static String eventSaveMessageFailed(String language) {
        String baseString = String.format(LocalisationService.getString("eventSaveMessageFailed", language),
                Emoji.FAILED.toString());
        return String.format(baseString, language);
    }

    private static String eventSaveMessageLocationFailed(String language) {
        String baseString = String.format(LocalisationService.getString("eventSaveMessageLocationFailed", language),
                Emoji.FAILED.toString(), Emoji.PIN.toString());
        return String.format(baseString, language);
    }

    private static String getLanguageMessage(String language) {
        String baseString = LocalisationService.getString("selectLanguage", language);
        return String.format(baseString, language);
    }

    // endregion Get Messages

    // region ReplyKeyboards

    private static ReplyKeyboardMarkup getMainMenuKeyboard(String language, boolean dbUserHasMobileNumber) {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(false);

        List<KeyboardRow> keyboard = new ArrayList<>();

        if (dbUserHasMobileNumber) {
            KeyboardRow keyboardEventsRow = new KeyboardRow();
            keyboardEventsRow.add(getEventsCommand(language));
            keyboard.add(keyboardEventsRow);
        } else {
            KeyboardRow keyboardShareContactRow = new KeyboardRow();
            KeyboardButton btnShareContact = new KeyboardButton();
            btnShareContact.setText(getShareContactCommand(language));
            btnShareContact.setRequestContact(true);
            keyboardShareContactRow.add(btnShareContact);
            keyboard.add(keyboardShareContactRow);
//        keyboardFirstRow.add(getShareContactCommand(language));
        }

        KeyboardRow keyboardThirdRow = new KeyboardRow();
        keyboardThirdRow.add(getSettingsCommand(language));
        keyboardThirdRow.add(getSupportCommand(language));

        keyboard.add(keyboardThirdRow);
        replyKeyboardMarkup.setKeyboard(keyboard);

        return replyKeyboardMarkup;
    }

    private static ReplyKeyboardMarkup getAddEventCategoryKeyboard(String language) {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(false);

        List<KeyboardRow> keyboard = new ArrayList<>();
        for (String categoryNameCode : LocalisationService.getCategories().stream().map(
                Category::getNameCode).collect(Collectors.toList())) {
            KeyboardRow row = new KeyboardRow();

            String categoryNameFmt = LocalisationService.getString(categoryNameCode, language);

            row.add(categoryNameFmt);
            keyboard.add(row);
        }

        KeyboardRow row = new KeyboardRow();
        row.add(getBackCommand(language));
        keyboard.add(row);

        replyKeyboardMarkup.setKeyboard(keyboard);

        return replyKeyboardMarkup;
    }

    private static ReplyKeyboardMarkup getAddEventNameKeyboard(String language) {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(false);

        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow backRow = new KeyboardRow();
        backRow.add(getBackCommand(language));
        keyboard.add(backRow);

        replyKeyboardMarkup.setKeyboard(keyboard);

        return replyKeyboardMarkup;
    }

    private static ReplyKeyboardMarkup getAddEventDescriptionKeyboard(String language) {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(false);

        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow backRow = new KeyboardRow();
        backRow.add(getBackCommand(language));
        keyboard.add(backRow);

        replyKeyboardMarkup.setKeyboard(keyboard);

        return replyKeyboardMarkup;
    }

    private static ReplyKeyboardMarkup getAddEventLocationKeyboard(String language) {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(false);

        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow backRow = new KeyboardRow();
        backRow.add(getBackCommand(language));
        keyboard.add(backRow);

        replyKeyboardMarkup.setKeyboard(keyboard);

        return replyKeyboardMarkup;
    }


    private static ReplyKeyboardMarkup getLanguagesKeyboard(String language, boolean onStart) {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(false);

        List<KeyboardRow> keyboard = new ArrayList<>();
        for (String languageName : LocalisationService.getSupportedLanguages().stream().map(
                Language::getName).collect(Collectors.toList())) {
            KeyboardRow row = new KeyboardRow();
            row.add(languageName);
            keyboard.add(row);
        }

        if (!onStart) {
            KeyboardRow row = new KeyboardRow();
            row.add(getCancelCommand(language));
            keyboard.add(row);
        }

        replyKeyboardMarkup.setKeyboard(keyboard);

        return replyKeyboardMarkup;
    }

    private static ReplyKeyboardMarkup getEventsKeyboard(String language) {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(false);

        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow keyboardFirstRow = new KeyboardRow();
        keyboardFirstRow.add(getAddEventCategoryCommand(language));

        KeyboardRow keyboardSecondRow = new KeyboardRow();

        keyboardSecondRow.add(getBackCommand(language));
        keyboard.add(keyboardFirstRow);
        keyboard.add(keyboardSecondRow);
        replyKeyboardMarkup.setKeyboard(keyboard);

        return replyKeyboardMarkup;
    }

    private static ReplyKeyboardMarkup getSettingsKeyboard(String language) {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(false);

        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow keyboardFirstRow = new KeyboardRow();
        keyboardFirstRow.add(getLanguagesCommand(language));

        KeyboardRow keyboardSecondRow = new KeyboardRow();

        keyboardSecondRow.add(getBackCommand(language));
        keyboard.add(keyboardFirstRow);
        keyboard.add(keyboardSecondRow);
        replyKeyboardMarkup.setKeyboard(keyboard);

        return replyKeyboardMarkup;
    }

    // endregion ReplyKeyboards

    // region getCommands


    private static String getSupportCommand(String language) {
        return String.format(LocalisationService.getString("support", language),
                Emoji.BLACK_QUESTION_MARK_ORNAMENT.toString());
    }

    private static String getLanguagesCommand(String language) {
        return String.format(LocalisationService.getString("languages", language),
                Emoji.GLOBE_WITH_MERIDIANS.toString());
    }

    private static String getBackCommand(String language) {
        return String.format(LocalisationService.getString("back", language),
                Emoji.BACK_WITH_LEFTWARDS_ARROW_ABOVE.toString());
    }

    private static String getSettingsCommand(String language) {
        return String.format(LocalisationService.getString("settings", language),
                Emoji.WRENCH.toString());
    }

    private static String getShareContactCommand(String language) {
        return String.format(LocalisationService.getString("shareContact", language),
                Emoji.MOBILE_PHONE.toString());
    }

    private static String getEventsCommand(String language) {
        return String.format(LocalisationService.getString("events", language), Emoji.EVENTS.toString());
    }

    private static String getAddEventCategoryCommand(String language) {
        return String.format(LocalisationService.getString("addEventCategory", language),
                Emoji.PENCIL.toString());
    }

    private static String getCancelCommand(String language) {
        return String.format(LocalisationService.getString("cancel", language),
                Emoji.CROSS_MARK.toString());
    }
    // endregion getCommands

    // region Send common messages

    private SendMessage sendMessageDefault(Message message, String language, boolean dbUserHasMobileNumber) {
        ReplyKeyboardMarkup replyKeyboardMarkup = getMainMenuKeyboard(language, dbUserHasMobileNumber);
        dbManager().insertLifeMapBotState(message.getFrom().getId(), message.getChatId(), STATE_MAIN_MENU);
        return sendHelpMessage(message.getChatId(), message.getMessageId(), replyKeyboardMarkup, language, dbUserHasMobileNumber);
    }

    private SendMessage sendShareContactMessage(Long chatId, Integer messageId, ReplyKeyboard replyKeyboard, String language) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(chatId);
        sendMessage.setReplyToMessageId(messageId);
        sendMessage.setReplyMarkup(replyKeyboard);
        sendMessage.setText(LocalisationService.getString("shareContactMessage", language));

        sendMessage.disableWebPagePreview();
        sendMessage.disableNotification();

        return sendMessage;
    }

    private SendMessage sendShareContactFailedMessage(Long chatId, Integer messageId, ReplyKeyboard replyKeyboard, String language) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(chatId);
        sendMessage.setReplyToMessageId(messageId);
        sendMessage.setReplyMarkup(replyKeyboard);
        sendMessage.setText(LocalisationService.getString("shareContactFailedMessage", language));

        sendMessage.disableWebPagePreview();
        sendMessage.disableNotification();

        return sendMessage;
    }

    private SendMessage sendChooseOptionMessage(Long chatId, Integer messageId,
                                                ReplyKeyboard replyKeyboard, String language) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(chatId.toString());
        sendMessage.setReplyToMessageId(messageId);
        sendMessage.setReplyMarkup(replyKeyboard);
        sendMessage.setText(LocalisationService.getString("chooseOption", language));

        sendMessage.disableWebPagePreview();

        return sendMessage;
    }

    private SendMessage sendHelpMessage(Long chatId, Integer messageId, ReplyKeyboardMarkup replyKeyboardMarkup, String language, boolean dbUserHasMobileNumber) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(chatId);
        sendMessage.setReplyToMessageId(messageId);
        if (replyKeyboardMarkup != null) {
            sendMessage.setReplyMarkup(replyKeyboardMarkup);
        }
        sendMessage.setText(getHelpMessage(language, dbUserHasMobileNumber));

        sendMessage.disableWebPagePreview();

        return sendMessage;
    }

    private SendMessage sendSupportMessage(Long chatId, Integer messageId, String language) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(chatId);
        sendMessage.setReplyToMessageId(messageId);

        sendMessage.setText(LocalisationService.getString("supportMessage", language));

        return sendMessage;
    }

    private SendMessage sendUpdateModerationStatusMessage(Long chatId, Integer messageId, ReplyKeyboard replyKeyboard, String language) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(chatId);
        sendMessage.setReplyToMessageId(messageId);
        sendMessage.setReplyMarkup(replyKeyboard);
        sendMessage.setText(LocalisationService.getString("updateModerationStatusMessage", language));

        sendMessage.disableWebPagePreview();
        sendMessage.disableNotification();

        return sendMessage;
    }

    private SendMessage sendUpdateModerationStatusFailedMessage(Long chatId, Integer messageId, ReplyKeyboard replyKeyboard, String language) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(chatId);
        sendMessage.setReplyToMessageId(messageId);
        sendMessage.setReplyMarkup(replyKeyboard);
        sendMessage.setText(LocalisationService.getString("updateModerationStatusFailedMessage", language));

        sendMessage.disableWebPagePreview();
        sendMessage.disableNotification();

        return sendMessage;
    }

    // endregion Send common messages

    private DatabaseManager dbManager() {
        return DatabaseManager.getInstance(this.config.getJsonObject("db"));
    }

    @Override
    public String getBotToken() {
        return BotConfig.DEV_BOT_TOKEN;
    }


    @Override
    public String getBotUsername() {
        return BotConfig.DEV_BOT_USER;
    }

}
