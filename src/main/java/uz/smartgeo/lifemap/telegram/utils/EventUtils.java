package uz.smartgeo.lifemap.telegram.utils;

import io.vertx.core.json.JsonObject;
import org.apache.log4j.Logger;

public class EventUtils {

    private static final Logger LOGGER = Logger.getLogger(EventUtils.class);

    public static String getEventStartDate(JsonObject address) {
        String startDate = "";

        return startDate;
    }

    @SuppressWarnings("DuplicateBranchesInSwitch")
    public static String getEventEndDateByCategory(int categoryId) {
        String endDate = DateUtils.getCurrentISOStringUTC();

        switch (categoryId) {
            case LIFEMAP_CONST.NEED_HELP:
                endDate = DateUtils.addDayToDate(endDate,3);
                break;
            case LIFEMAP_CONST.READY_ASSIST:
                endDate = DateUtils.addDayToDate(endDate,7);
                break;
            case LIFEMAP_CONST.WATER:
                endDate = DateUtils.addDayToDate(endDate,3);
                break;
            case LIFEMAP_CONST.FOOD:
                endDate = DateUtils.addDayToDate(endDate,3);
                break;
            case LIFEMAP_CONST.HEALTH_CARE_CENTER:
                endDate = DateUtils.addDayToDate(endDate,10);
                break;
            case LIFEMAP_CONST.DOCTOR:
                endDate = DateUtils.addDayToDate(endDate,10);
                break;
            case LIFEMAP_CONST.BLOOD_CENTER:
                endDate = DateUtils.addDayToDate(endDate,10);
                break;
            case LIFEMAP_CONST.BLOOD_DONOR:
                endDate = DateUtils.addDayToDate(endDate,3);
                break;
            case LIFEMAP_CONST.CHARITY:
                endDate = DateUtils.addDayToDate(endDate,3);
                break;
            case LIFEMAP_CONST.ATTENTION:
                endDate = DateUtils.addDayToDate(endDate,3);
                break;
            case LIFEMAP_CONST.LEVEL:
                endDate = DateUtils.addDayToDate(endDate,7);
                break;
            case LIFEMAP_CONST.VET:
                endDate = DateUtils.addDayToDate(endDate,10);
                break;
        }

        return endDate;
    }

}
