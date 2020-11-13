package uz.smartgeo.lifemap.telegram.utils;

public class LIFEMAP_CONST {

    public static final long PER_PAGE = 3L;
    public static final int PER_PAGE_500 = 500;

    public static final int SQL_UPDATED_SUCCESS = 1;
    public static final int OPERATION_TYPE_UPDATE = 2;
    public static final int OPERATION_TYPE_INSERT = 3;

    public static final String TABLE_NAME_USER = "lm_user";
    public static final String TABLE_NAME_ROLE_PERMISSION = "lm_role_permission";
    public static final String TABLE_NAME_EVENT_CATEGORY = "lm_event_category";
    public static final String TABLE_NAME_EVENT = "lm_event";

    public static final long start = 0;
    public static final long end = -1;
    public static final long HOUR = 3600000;
    public static final long DAY = 86400000;

    public static final int ROLE_SYSTEM_ADMINISTRATOR = 1;
    public static final int ROLE_ADMINISTRATOR = 2;
    public static final int ROLE_MODERATOR = 3;
    public static final int ROLE_USER = 4;
    public static final int ROLE_TRUSTED_USER = 5;

    public static final String DEFAULT_LANGUAGE = "en";

//Event Categories
    public static final int NEED_HELP = 1;
    public static final int READY_ASSIST = 2;
    public static final int WATER = 3;
    public static final int FOOD = 4;
    public static final int HEALTH_CARE_CENTER = 5;
    public static final int DOCTOR = 6;
    public static final int BLOOD_CENTER = 7;
    public static final int BLOOD_DONOR = 8;
    public static final int CHARITY = 9;
    public static final int ATTENTION = 10;
    public static final int LEVEL = 11;
    public static final int VET = 12;

}


