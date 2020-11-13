package uz.smartgeo.lifemap.telegram.utils;

import io.vertx.core.json.JsonObject;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Stanislav on 24.07.20.
 */
public class Converters {

    public static Timestamp dateFormatterTimestampFull(String dateFrom) {
        SimpleDateFormat dateFormatterFrom = new SimpleDateFormat("dd.MM.yyyy HH:mm");
        Date convertedDate;
        Timestamp timestamp = null;
        try {
            convertedDate = dateFormatterFrom.parse(dateFrom);
            timestamp = new Timestamp(convertedDate.getTime());
        } catch (ParseException e) {
            //        e.printStackTrace();
        }

        return timestamp;
    }

    /**
     * UTC +05  method
     * @param strDateTimeISO String
     * @return String
     */
    public static String getDateTimeFromISO(String strDateTimeISO) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        ZonedDateTime localDateTime = ZonedDateTime.parse(strDateTimeISO);
        ZonedDateTime utcDate = localDateTime.withZoneSameInstant(ZoneId.of("Asia/Tashkent"));
        return utcDate.format(formatter);
    }

    //Returns a json object from an input stream
    public static JsonObject getJsonObject(InputStream inputStream) {

        try {
            BufferedReader streamReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            StringBuilder responseStrBuilder = new StringBuilder();

            String inputStr;
            while ((inputStr = streamReader.readLine()) != null)
                responseStrBuilder.append(inputStr);

            JsonObject jsonObject = new JsonObject(responseStrBuilder.toString());

            //returns the json object
            return jsonObject;

        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }

        //if something went wrong, return null
        return null;
    }

    public static boolean convertStrBoolean(String val) {
        switch (val) {
            case "false":
                return false;
            case "true":
                return true;
            default:
                return false;
        }
    }
}
