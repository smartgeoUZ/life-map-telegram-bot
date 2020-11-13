package uz.smartgeo.lifemap.telegram.database;

/**
 * @author Ruben Bermudez
 * @version 2.0
 * @brief Strings to create uz.smartgeo.lifemap.telegram.database
 * @date 15 of May of 2015
 */
public class CreationStrings {
    public static final int version = 8;

    public static final String createUserStateTable = " create table if not exists public.lm_bot_user_state " +
            " (user_id bigint, " +
            " chat_id bigint, " +
            " state integer default 0 not null, " +
            " lang text default 'en', " +
            " constraint lm_bot_state_pk " +
            " unique (user_id, chat_id), " +
            " status char default 'A'::bpchar, " +
            " reg_date timestamp default now(), " +
            " mod_date timestamp, " +
            " exp_date timestamp);" +
            " alter table public.lm_bot_user_state owner to lifemap;";


    public static final String createUserOptionTable = "CREATE TABLE IF NOT EXISTS lm_bot_user_option " +
            " (user_id BIGINT PRIMARY KEY, " +
            " lang  text, " +
            " units text, " +
            " status char default 'A'::bpchar, " +
            " reg_date timestamp default now(), " +
            " mod_date timestamp, " +
            " exp_date timestamp)";

}
