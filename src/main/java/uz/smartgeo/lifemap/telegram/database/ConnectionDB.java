/*
 * This is the source code of Telegram Bot v. 2.0
 * It is licensed under GNU GPL v. 3 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Ruben Bermudez, 3/12/14.
 */
package uz.smartgeo.lifemap.telegram.database;

import io.vertx.core.json.JsonObject;
import org.apache.log4j.Logger;

import java.sql.*;

/**
 * @author Ruben Bermudez
 * @version 2.0
 * Connector to uz.smartgeo.lifemap.telegram.database
 */
public class ConnectionDB {
    private static final Logger LOGGER = Logger.getLogger(DatabaseManager.class);

    private static String DRIVER_NAME;
    private static String DB_URL;
    private static String DB_USER_NAME;
    private static String DB_USER_PASSWORD;

    private Connection currentConnection;

    public ConnectionDB(JsonObject dbConfig) {
        DRIVER_NAME = dbConfig.getString("driver");
        DB_URL = dbConfig.getString("url");
        DB_USER_NAME = dbConfig.getString("username");
        DB_USER_PASSWORD = dbConfig.getString("password");

        this.currentConnection = openConnection();
    }

    private Connection openConnection() {
        Connection connection = null;
        try {
            Class.forName("org.postgresql.Driver").newInstance();
            connection = DriverManager.getConnection(DB_URL, DB_USER_NAME, DB_USER_PASSWORD);
        } catch (SQLException | ClassNotFoundException | IllegalAccessException | InstantiationException e) {
            LOGGER.error("openConnection error", e);
        }

        return connection;
    }

    public void reopenConnection() {
        try {
            this.currentConnection = null;

            Class.forName("org.postgresql.Driver").newInstance();
            this.currentConnection = DriverManager.getConnection(DB_URL, DB_USER_NAME, DB_USER_PASSWORD);
        } catch (SQLException | ClassNotFoundException | IllegalAccessException | InstantiationException e) {
            LOGGER.error("openConnection error", e);
        }
    }

    public void closeConnection() {
        try {
            this.currentConnection.close();
        } catch (SQLException e) {
            LOGGER.error("closeConnection error", e);
        }
    }

    public Boolean executeQuery(String query) {
        try {
            final Statement statement = this.currentConnection.createStatement();
            return statement.execute(query);
        } catch (SQLException e) {
            LOGGER.error("ConnectionDB: " + e);
        }
        return false;
    }

    public ResultSet executeQuery(PreparedStatement preparedStatement) {
        try {

            if (this.isClosed()) {
                LOGGER.info("--isClosed--: " + this.isClosed());
                LOGGER.info("--isValid--: " + this.isValid());

                LOGGER.info("TS: " + new Timestamp(System.currentTimeMillis()));

                this.reopenConnection();
                LOGGER.info("TS: " + new Timestamp(System.currentTimeMillis()));

                if (this.isClosed()) {
                    LOGGER.info("TS: " + new Timestamp(System.currentTimeMillis()));
                    LOGGER.info("isClosed: " + this.isClosed());
                    LOGGER.info("isValid: " + this.isValid());
                }

            }

            if (!this.isValid()) {
                LOGGER.info("--isClosed--: " + this.isClosed());
                LOGGER.info("--isValid--: " + this.isValid());

                LOGGER.info("TS: " + new Timestamp(System.currentTimeMillis()));

                this.reopenConnection();
                LOGGER.info("TS: " + new Timestamp(System.currentTimeMillis()));

                if (!this.isValid()) {
                    LOGGER.info("TS: " + new Timestamp(System.currentTimeMillis()));
                    LOGGER.info("isClosed: " + this.isClosed());
                    LOGGER.info("isValid: " + this.isValid());
                }
            }

            return preparedStatement.executeQuery();
        } catch (SQLException e) {
            LOGGER.error("ConnectionDB: " + e);
        }
        return null;
    }

    public int executeUpdate(PreparedStatement preparedStatement) {
        int updatedRows = 0;

        try {
            if (this.isClosed()) {
                LOGGER.info("--isClosed--: " + this.isClosed());
                LOGGER.info("--isValid--: " + this.isValid());

                LOGGER.info("TS: " + new Timestamp(System.currentTimeMillis()));

                this.reopenConnection();
                LOGGER.info("TS: " + new Timestamp(System.currentTimeMillis()));

                if (this.isClosed()) {
                    LOGGER.info("TS: " + new Timestamp(System.currentTimeMillis()));
                    LOGGER.info("isClosed: " + this.isClosed());
                    LOGGER.info("isValid: " + this.isValid());
                }

            }

            if (!this.isValid()) {
                LOGGER.info("--isClosed--: " + this.isClosed());
                LOGGER.info("--isValid--: " + this.isValid());

                LOGGER.info("TS: " + new Timestamp(System.currentTimeMillis()));

                this.reopenConnection();
                LOGGER.info("TS: " + new Timestamp(System.currentTimeMillis()));

                if (!this.isValid()) {
                    LOGGER.info("TS: " + new Timestamp(System.currentTimeMillis()));
                    LOGGER.info("isClosed: " + this.isClosed());
                    LOGGER.info("isValid: " + this.isValid());
                }
            }

            updatedRows = preparedStatement.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("ConnectionDB: " + e);
        }
        return updatedRows;
    }

    public PreparedStatement getPreparedStatement(String query) throws SQLException {

        if (this.currentConnection == null) {
            this.reopenConnection();
        }

        if (this.isClosed()) {
            LOGGER.info("--isClosed--: " + this.isClosed());
            LOGGER.info("--isValid--: " + this.isValid());

            LOGGER.info("TS: " + new Timestamp(System.currentTimeMillis()));

            this.reopenConnection();
            LOGGER.info("TS: " + new Timestamp(System.currentTimeMillis()));

            if (this.isClosed()) {
                LOGGER.info("TS: " + new Timestamp(System.currentTimeMillis()));
                LOGGER.info("isClosed: " + this.isClosed());
                LOGGER.info("isValid: " + this.isValid());
            }

        }

        if (!this.isValid()) {
            LOGGER.info("--isClosed--: " + this.isClosed());
            LOGGER.info("--isValid--: " + this.isValid());
            LOGGER.info("TS: " + new Timestamp(System.currentTimeMillis()));

            this.reopenConnection();
            LOGGER.info("TS: " + new Timestamp(System.currentTimeMillis()));

            if (!this.isValid()) {
                LOGGER.info("TS: " + new Timestamp(System.currentTimeMillis()));
                LOGGER.info("isClosed: " + this.isClosed());
                LOGGER.info("isValid: " + this.isValid());
            }
        }

        return this.currentConnection.prepareStatement(query);
    }

    public PreparedStatement getPreparedStatement(String query, int flags) throws SQLException {
        return this.currentConnection.prepareStatement(query, flags);
    }

    /**
     * Initilize a transaction in uz.smartgeo.lifemap.telegram.database
     *
     * @throws SQLException If initialization fails
     */
    public void initTransaction() throws SQLException {
        this.currentConnection.setAutoCommit(false);
    }


    public void rollbackTransaction() {
        try {
            this.currentConnection.rollback();
        } catch (SQLException e) {
            LOGGER.error("ConnectionDB: " + e);
        }
    }

    public Boolean isValid() {
        try {
            return this.currentConnection.isValid(2);
        } catch (SQLException e) {
            LOGGER.error("ConnectionDB: " + e);
        }
        return false;

    }

    public Boolean isClosed() {
        try {
            return this.currentConnection.isClosed();
        } catch (SQLException e) {
            LOGGER.error("ConnectionDB: " + e);
        }
        return false;
    }

//    /**
//     * Finish a transaction in uz.smartgeo.lifemap.telegram.database and commit changes
//     *
//     * @throws SQLException If a rollback fails
//     */
//    public void commitTransaction() throws SQLException {
//        try {
//            try {
//                this.currentConnection.commit();
//            } catch (SQLException e) {
//                if (this.currentConnection != null) {
//                    this.currentConnection.rollback();
//                }
//            } finally {
//                this.currentConnection.setAutoCommit(false);
//            }
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//    }
}
