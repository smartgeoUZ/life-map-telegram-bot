package uz.smartgeo.lifemap.telegram.utils;

import org.apache.log4j.Logger;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class DateUtils {

    private static final Logger LOGGER = Logger.getLogger(DateUtils.class);

    public static String getCurrentISOStringUTC() {
        String nowAsISO = null;

        try {
            TimeZone tz = TimeZone.getTimeZone("UTC");
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'"); // Quoted "Z" to indicate UTC, no timezone offset
            df.setTimeZone(tz);
            nowAsISO = df.format(new Date());

            return nowAsISO;
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.error(e.getMessage());
        }

        return nowAsISO;
    }

    public static String getISOString(String dateStr) {
        String isoString = dateStr;

        try {
            TimeZone tz = TimeZone.getTimeZone("UTC");
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'"); // Quoted "Z" to indicate UTC, no timezone offset
            df.setTimeZone(tz);

            DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date date = format.parse(dateStr);

            isoString = df.format(date);

            return isoString;
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.error(e.getMessage());
        }

        return isoString;
    }
//
//    public static String getCurrentIsoString() {
//        String nowAsISO = null;
//
//        try {
//            TimeZone tz = TimeZone.getTimeZone("Asia/Tashkent");
//            DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'"); // Quoted "Z" to indicate UTC, no timezone offset
//            df.setTimeZone(tz);
//            nowAsISO = df.format(new Date());
//
//            return nowAsISO;
//        } catch (Exception e) {
//            e.printStackTrace();
//            LOGGER.error(e.getMessage());
//        }
//
//        return nowAsISO;
//    }

    public static String addDayToDate(String dateStr, int days) {
        String date = null;

        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
            Calendar c = Calendar.getInstance();

            c.setTime(dateFormat.parse(dateStr));

            c.add(Calendar.DATE, days);  // number of days to add
            date = dateFormat.format(c.getTime());  // dt is now the new date
            return date;
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.error(e.getMessage());
        }

        return date;
    }

    static public String addOneDay(String dateStr) {
        return LocalDate.parse(dateStr).plusDays(1).toString();
    }

    public static Date addDays(Date date, int days) {
        try {
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            cal.add(Calendar.DATE, days); //minus number would decrement the days
            return cal.getTime();
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.error(e.getMessage());
        }
        return null;
    }

    public static String cutDateStrSeconds(String dateStr) {
        SimpleDateFormat fromDateStr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        SimpleDateFormat toDateStr = new SimpleDateFormat("yyyy-MM-dd HH:mm");

        try {
            return toDateStr.format(fromDateStr.parse(dateStr));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

}
