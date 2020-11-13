
package uz.smartgeo.lifemap.telegram.database;

import io.vertx.core.json.JsonObject;
import org.apache.log4j.Logger;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    private static final Logger LOGGER = Logger.getLogger(DatabaseManager.class);

    private static volatile DatabaseManager instance;
    private static volatile ConnectionDB connection;

    JsonObject dbConfig;

    /**
     * Private constructor (due to Singleton)
     */
    private DatabaseManager(JsonObject conf) {
        this.dbConfig = conf;
        connection = new ConnectionDB(dbConfig);

        recreateTable();

//                final int currentVersion = connection.checkVersion();
//
//                if (currentVersion < CreationStrings.version) {
//                    recreateTable(currentVersion);
//                }
    }

    /**
     * Get Singleton instance
     *
     * @return instance of the class
     */
    public static DatabaseManager getInstance(JsonObject dbConfig) {
        final DatabaseManager currentInstance;
        if (instance == null) {
            synchronized (DatabaseManager.class) {
                if (instance == null) {
                    try {
                        instance = new DatabaseManager(dbConfig);
                    } catch (Exception e) {
                        LOGGER.error("DatabaseManager" + e.getMessage());
                    }
                }
                currentInstance = instance;
            }
        } else {
            currentInstance = instance;
        }
        return currentInstance;
    }

    /**
     * Recreates the DB
     */
    @SuppressWarnings("ConstantConditions")
    private void recreateTable() {
        try {

//            connection.initTransaction();

            int currentVersion = 1;

            if (currentVersion == 999) {
                createNewTables();
            }

//            connection.commitTransaction();

        } catch (Exception e) {
            connection.rollbackTransaction();
            LOGGER.error("recreateTable error: ", e);
        }
    }

    private int createNewTables() {
        connection.executeQuery(CreationStrings.createUserStateTable);
        connection.executeQuery(CreationStrings.createUserOptionTable);

        return CreationStrings.version;
    }


    public int getLifeMapBotState(Integer userId, Long chatId) {
        int state = 0;
        try {
            final PreparedStatement preparedStatement = connection.getPreparedStatement("SELECT state FROM lm_bot_user_state WHERE user_id = ? AND chat_id = ?");
            preparedStatement.setLong(1, userId);
            preparedStatement.setLong(2, chatId);
            final ResultSet result = connection.executeQuery(preparedStatement);
            if (result.next()) {
                state = result.getInt("state");
            }
        } catch (SQLException e) {
            connection.rollbackTransaction();
            LOGGER.error("DatabaseManager" + e.getMessage());
        }
        return state;
    }

    public boolean addBotEvent(Integer telegramUserId, Long dbUserId, Integer categoryId) {
        int updatedRows = 0;
        try {
            final PreparedStatement preparedStatement = connection.getPreparedStatement("" +
                    " INSERT INTO lm_bot_event " +
                    " (user_id, ext_user_id, category_id) " +
                    " VALUES (?, ?, ?);");

            preparedStatement.setLong(1, dbUserId);
            preparedStatement.setLong(2, telegramUserId);
            preparedStatement.setLong(3, categoryId);
            updatedRows = connection.executeUpdate(preparedStatement);
        } catch (SQLException e) {
            connection.rollbackTransaction();
            LOGGER.error("DatabaseManager" + e.getMessage());
        }

        return updatedRows > 0;
    }

    public boolean updateBotEventName(Integer telegramUserId, Long dbUserId, String name) {
        int updatedRows = 0;
        try {
            final PreparedStatement preparedStatement = connection.getPreparedStatement("" +
                    "  UPDATE lm_bot_event " +
                    "  SET name     = ?, " +
                    "  mod_date = now() " +
                    "  WHERE id = ( " +
                    "    SELECT id " +
                    "    FROM lm_bot_event " +
                    "    WHERE user_id = ? " +
                    "      and ext_user_id = ? " +
                    "      and (is_imported is null or is_imported = false) " +
                    "      and complete_date is null " +
                    "      and status = 'A' " +
                    "    order by reg_date desc " +
                    "    LIMIT 1 " +
                    ") ");

            preparedStatement.setString(1, name);
            preparedStatement.setLong(2, dbUserId);
            preparedStatement.setLong(3, telegramUserId);

            updatedRows = connection.executeUpdate(preparedStatement);
        } catch (SQLException e) {
            connection.rollbackTransaction();
            LOGGER.error("DatabaseManager" + e.getMessage());
        }

        return updatedRows > 0;
    }

    public boolean updateBotEventDescription(Integer telegramUserId, Long dbUserId, String description) {
        int updatedRows = 0;
        try {
            final PreparedStatement preparedStatement = connection.getPreparedStatement("" +
                    "  UPDATE lm_bot_event " +
                    "  SET description = ?, " +
                    "  mod_date = now() " +
                    "  WHERE id = ( " +
                    "    SELECT id " +
                    "    FROM lm_bot_event " +
                    "    WHERE user_id = ? " +
                    "      and ext_user_id = ? " +
                    "      and (is_imported is null or is_imported = false) " +
                    "      and complete_date is null " +
                    "      and status = 'A' " +
                    "    order by reg_date desc " +
                    "    LIMIT 1 " +
                    ") ");

            preparedStatement.setString(1, description);
            preparedStatement.setLong(2, dbUserId);
            preparedStatement.setLong(3, telegramUserId);

            updatedRows = connection.executeUpdate(preparedStatement);
        } catch (SQLException e) {
            connection.rollbackTransaction();
            LOGGER.error("DatabaseManager" + e.getMessage());
        }

        return updatedRows > 0;
    }

    public long updateBotEventGeoJson(Integer telegramUserId, Long dbUserId, String geoJson,
                                      String address,
                                      String startDate, String endDate,
                                      String country, String region) {
        int updatedRows = 0;
        long addedBotEventId = 0;

        try {
            final PreparedStatement preparedStatement = connection.getPreparedStatement("" +
                    "  UPDATE lm_bot_event " +
                    "  SET geojson = CAST(? AS json), " +
                    "  address = ?, " +
                    "  country_code = ?, " +
                    "  region =?, " +
                    "  start_date = CAST(? AS timestamptz), " +
                    "  end_date = CAST(? AS timestamptz), " +
                    "  mod_date = now(), " +
                    "  complete_date = now() " +
                    "  WHERE id = ( " +
                    "    SELECT id " +
                    "    FROM lm_bot_event " +
                    "    WHERE user_id = ? " +
                    "      and ext_user_id = ? " +
                    "      and (is_imported is null or is_imported = false) " +
                    "      and complete_date is null " +
                    "      and status = 'A' " +
                    "    order by reg_date desc " +
                    "    LIMIT 1  " +
                    ") returning id ");

            preparedStatement.setString(1, geoJson);
            preparedStatement.setString(2, address);
            preparedStatement.setString(3, country);
            preparedStatement.setString(4, region);
            preparedStatement.setString(5, startDate);
            preparedStatement.setString(6, endDate);
            preparedStatement.setLong(7, dbUserId);
            preparedStatement.setLong(8, telegramUserId);

            ResultSet res = connection.executeQuery(preparedStatement);

            if (res.next()) {
                addedBotEventId = res.getLong(1);
                System.out.println("Inserted ID -" + addedBotEventId); // display inserted record
            }

        } catch (SQLException e) {
            connection.rollbackTransaction();
            LOGGER.error("DatabaseManager" + e.getMessage());
        }

        return addedBotEventId;
    }

    public boolean importTrack(Long botEventId, Long regionId) {
        int updatedRows = 0;

        try {
            final PreparedStatement preparedStatement = connection.getPreparedStatement("" +
                    "  INSERT INTO lm_event (user_id, category_id, name, description, start_date, end_date, country_code, region, address, geojson, bot_event_id, region_id) " +
                    "    (SELECT be.user_id, " +
                    "            be.category_id, " +
                    "            be.name, " +
                    "            be.description, " +
                    "            be.start_date, " +
                    "            be.end_date, " +
                    "            be.country_code, " +
                    "            be.region, " +
                    "            be.address, " +
                    "            be.geojson, " +
                    "            be.id, " +
                    "            ? " +
                    "     FROM lm_bot_event be " +
                    "     WHERE be.id = ? " +
                    "       and be.complete_date is not null " +
                    "       and be.is_imported is null);  ");

            preparedStatement.setLong(1, regionId);
            preparedStatement.setLong(2, botEventId);

            updatedRows = connection.executeUpdate(preparedStatement);
        } catch (SQLException e) {
            connection.rollbackTransaction();
            LOGGER.error("DatabaseManager" + e.getMessage());
        }

        return updatedRows > 0;
    }

    public boolean updateBotEventImportValues(Long botEventId) {
        int updatedRows = 0;
        try {
            final PreparedStatement preparedStatement = connection.getPreparedStatement("" +
                    "  UPDATE lm_bot_event " +
                    "  SET is_imported  = true, " +
                    "  imported_date = now(), " +
                    "  mod_date = now() " +
                    "  WHERE id = ?; ");

            preparedStatement.setLong(1, botEventId);

            updatedRows = connection.executeUpdate(preparedStatement);
        } catch (SQLException e) {
            connection.rollbackTransaction();
            LOGGER.error("DatabaseManager" + e.getMessage());
        }

        return updatedRows > 0;
    }

    public boolean insertLifeMapBotState(Integer userId, Long chatId, int state) {
        int updatedRows = 0;
        try {
            final PreparedStatement preparedStatement = connection.getPreparedStatement("INSERT INTO lm_bot_user_state" +
                    " (user_id, chat_id, state) VALUES (?, ?, ?) ON CONFLICT (user_id, chat_id) DO UPDATE SET state = ?;");
            preparedStatement.setLong(1, userId);
            preparedStatement.setLong(2, chatId);
            preparedStatement.setInt(3, state);
            preparedStatement.setInt(4, state);
            updatedRows = connection.executeUpdate(preparedStatement);
        } catch (SQLException e) {
            connection.rollbackTransaction();
            LOGGER.error("DatabaseManager" + e.getMessage());
        }

        return updatedRows > 0;
    }

    public boolean saveUser(Integer telegramUserId, String login, String firstName, String lastName, String phoneNumber) {
        int updatedRows = 0;
        try {

            final PreparedStatement preparedStatement = connection.getPreparedStatement(" " +
                    " INSERT INTO lm_user (login, first_name, last_name, ext_user_id," +
                    " auth_type_id, photo_url, phone_mobile, last_logged_in) " +
                    " select ?, ?, ?, ?, ?, ?, ?, now() " +
                    " WHERE not exists(select * from lm_user where ext_user_id = ? and status = 'A' );");

            preparedStatement.setString(1, login);
            preparedStatement.setString(2, firstName);
            preparedStatement.setString(3, lastName);
            preparedStatement.setLong(4, telegramUserId);
            preparedStatement.setLong(5, 1);
            preparedStatement.setString(6, "");
            preparedStatement.setString(7, phoneNumber);
            preparedStatement.setLong(8, telegramUserId);

            updatedRows = connection.executeUpdate(preparedStatement);
        } catch (SQLException e) {
            connection.rollbackTransaction();
            LOGGER.error("DatabaseManager" + e.getMessage());
        }

        return updatedRows > 0;
    }

    public List<String> getUserOptions(Integer userId) {
        List<String> options = new ArrayList<>();
        try {

            final PreparedStatement preparedStatement = connection.getPreparedStatement("SELECT * FROM lm_bot_user_option WHERE user_id = ?");
            preparedStatement.setLong(1, userId);
            final ResultSet result = connection.executeQuery(preparedStatement);
            if (result.next()) {
                options.add(result.getString("lang"));
                options.add(result.getString("is_start_text_shown"));
            } else {
//                addNewUserLanguageOption(userId, "en");
            }
        } catch (SQLException e) {
            connection.rollbackTransaction();
            LOGGER.error("DatabaseManager" + e.getMessage());
        }
        return options;
    }

    public JsonObject getBotEvent(Long botBotEventId) {
        JsonObject options = new JsonObject();
        try {

            final PreparedStatement preparedStatement = connection.getPreparedStatement("SELECT * FROM lm_bot_event WHERE id = ?");
            preparedStatement.setLong(1, botBotEventId);
            final ResultSet result = connection.executeQuery(preparedStatement);

            if (result.next()) {
                options.put("name", result.getString("name"));
                options.put("description", result.getString("description"));
                options.put("address", result.getString("address"));
                options.put("start_date", result.getString("start_date"));
                options.put("end_date", result.getString("end_date"));
                options.put("reg_date", result.getString("reg_date"));
                options.put("address", result.getString("address"));
            }

        } catch (SQLException e) {
            connection.rollbackTransaction();
            LOGGER.error("DatabaseManager" + e.getMessage());
        }
        return options;
    }


    public boolean putUserLanguageOption(Integer userId, String language) {
        int updatedRows = 0;
        try {
//            final PreparedStatement preparedStatement = connection.getPreparedStatement("UPDATE lm_bot_user_option SET lang = ? WHERE user_id = ?");
            final PreparedStatement preparedStatement = connection.getPreparedStatement("" +
                    " INSERT INTO lm_bot_user_option (user_id, lang)" +
                    " VALUES (?,?) " +
                    " ON CONFLICT (user_id) DO UPDATE SET lang = ?; ");
            preparedStatement.setLong(1, userId);
            preparedStatement.setString(2, language);
            preparedStatement.setString(3, language);
            updatedRows = connection.executeUpdate(preparedStatement);
        } catch (SQLException e) {
            connection.rollbackTransaction();
            LOGGER.error("DatabaseManager" + e.getMessage());
        }
        return updatedRows > 0;
    }

    /**
     * Return region id by coordinates
     * @param lon longitude
     * @param lat latitude
     * @return regionId
     */
    public long getRegionIdByCoordinates(Double lon, Double lat) {
        long regionId = 0L;

        try {
            final PreparedStatement preparedStatement = connection.getPreparedStatement("" +
                    "   select * from get_region_by_location(?, ?) as region_id;  ");

            preparedStatement.setDouble(1, lon);
            preparedStatement.setDouble(2, lat);

            final ResultSet result = connection.executeQuery(preparedStatement);
            if (result.next()) {
                regionId = result.getLong("region_id");
                LOGGER.info(regionId);
            }

        } catch (SQLException e) {
            connection.rollbackTransaction();
            LOGGER.error("DatabaseManager" + e.getMessage());
        }

        return regionId;
    }


}
