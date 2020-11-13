package uz.smartgeo.lifemap.telegram.services;

import uz.smartgeo.lifemap.telegram.utils.Category;
import uz.smartgeo.lifemap.telegram.utils.LIFEMAP_CONST;
import uz.smartgeo.lifemap.telegram.utils.Language;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

/**
 * @author Ruben Bermudez
 * @version 1.0
 */
public class LocalisationService {
    private static final String STRINGS_FILE = "strings";
    private static final Object lock = new Object();

    private static final List<Language> supportedLanguages = new ArrayList<>();
    private static final List<Category> categories = new ArrayList<>();

    private static final Utf8ResourceBundle defaultLanguage;
    private static final Utf8ResourceBundle russian;
    private static final Utf8ResourceBundle uzbek;


    static {
        synchronized (lock) {
            defaultLanguage = new Utf8ResourceBundle(STRINGS_FILE, Locale.ROOT);
            russian = new Utf8ResourceBundle(STRINGS_FILE, new Locale("ru", "ru"));
            uzbek = new Utf8ResourceBundle(STRINGS_FILE, new Locale("uz", "uz"));

            supportedLanguages.add(new Language("en", "English"));
            supportedLanguages.add(new Language("ru", "Русский"));
            supportedLanguages.add(new Language("uz", "Uzbek"));

            categories.add(new Category(1, "eventCategoryNeedHelp",
                    getString("eventCategoryNeedHelp", "en"),
                    getString("eventCategoryNeedHelp", "ru"),
                    getString("eventCategoryNeedHelp", "uz")));

            categories.add(new Category(2, "eventCategoryReadyToHelp",
                    getString("eventCategoryReadyToHelp", "en"),
                    getString("eventCategoryReadyToHelp", "ru"),
                    getString("eventCategoryReadyToHelp", "uz")));

            categories.add(new Category(3, "eventCategoryWater",
                    getString("eventCategoryWater", "en"),
                    getString("eventCategoryWater", "ru"),
                    getString("eventCategoryWater", "uz")));

            categories.add(new Category(4, "eventCategoryFood",
                    getString("eventCategoryFood", "en"),
                    getString("eventCategoryFood", "ru"),
                    getString("eventCategoryFood", "uz")));

            categories.add(new Category(5, "eventCategoryHealthCareCenter",
                    getString("eventCategoryHealthCareCenter", "en"),
                    getString("eventCategoryHealthCareCenter", "ru"),
                    getString("eventCategoryHealthCareCenter", "uz")));

            categories.add(new Category(6, "eventCategoryDoctor",
                    getString("eventCategoryDoctor", "en"),
                    getString("eventCategoryDoctor", "ru"),
                    getString("eventCategoryDoctor", "uz")));

            categories.add(new Category(7, "eventCategoryBloodCenter",
                    getString("eventCategoryBloodCenter", "en"),
                    getString("eventCategoryBloodCenter", "ru"),
                    getString("eventCategoryBloodCenter", "uz")));

            categories.add(new Category(8, "eventCategoryBloodDonor",
                    getString("eventCategoryBloodDonor", "en"),
                    getString("eventCategoryBloodDonor", "ru"),
                    getString("eventCategoryBloodDonor", "uz")));

            categories.add(new Category(9, "eventCategoryCharity",
                    getString("eventCategoryCharity", "en"),
                    getString("eventCategoryCharity", "ru"),
                    getString("eventCategoryCharity", "uz")));

            categories.add(new Category(10, "eventCategoryAttention",
                    getString("eventCategoryAttention", "en"),
                    getString("eventCategoryAttention", "ru"),
                    getString("eventCategoryAttention", "uz")));

         categories.add(new Category(11, "eventCategoryDriver",
                    getString("eventCategoryDriver", "en"),
                    getString("eventCategoryDriver", "ru"),
                    getString("eventCategoryDriver", "uz")));

         categories.add(new Category(12, "eventCategoryVet",
                    getString("eventCategoryVet", "en"),
                    getString("eventCategoryVet", "ru"),
                    getString("eventCategoryVet", "uz")));

        }
    }

    /**
     * Get a string in default language (en)
     *
     * @param key key of the resource to fetch
     * @return fetched string or error message otherwise
     */
    public static String getString(String key) {
        String result;
        try {
            result = defaultLanguage.getString(key);
        } catch (MissingResourceException e) {
            result = "String not found";
        }

        return result;
    }

    /**
     * Get a string in default language
     *
     * @param key key of the resource to fetch
     * @return fetched string or error message otherwise
     */
    public static String getString(String key, String language) {
        String result;
        try {
            switch (language.toLowerCase()) {
                case "ru":
                    result = russian.getString(key);
                    break;
                case "uz":
                    result = uzbek.getString(key);
                    break;
                default:
                    result = defaultLanguage.getString(key);
                    break;
            }
        } catch (MissingResourceException e) {
            result = defaultLanguage.getString(key);
        }

        return result;
    }

    public static List<Language> getSupportedLanguages() {
        return supportedLanguages;
    }

    public static List<Category> getCategories() {
        return categories;
    }

    public static String getCategoryNameById(int categoryId, String lang) {
        String categoryStr;

        switch (categoryId) {
            case LIFEMAP_CONST.NEED_HELP:
                categoryStr = getString("eventCategoryNeedHelp", lang);
                break;
            case LIFEMAP_CONST.READY_ASSIST:
                categoryStr = getString("eventCategoryReadyToHelp", lang);
                break;
            case LIFEMAP_CONST.WATER:
                categoryStr = getString("eventCategoryWater", lang);
                break;
            case LIFEMAP_CONST.FOOD:
                categoryStr = getString("eventCategoryFood", lang);
                break;
            case LIFEMAP_CONST.HEALTH_CARE_CENTER:
                categoryStr = getString("eventCategoryHealthCareCenter", lang);
                break;
            case LIFEMAP_CONST.DOCTOR:
                categoryStr = getString("eventCategoryDoctor", lang);
                break;
            case LIFEMAP_CONST.BLOOD_CENTER:
                categoryStr = getString("eventCategoryBloodCenter", lang);
                break;
            case LIFEMAP_CONST.BLOOD_DONOR:
                categoryStr = getString("eventCategoryBloodDonor", lang);
                break;
            case LIFEMAP_CONST.CHARITY:
                categoryStr = getString("eventCategoryCharity", lang);
                break;
            case LIFEMAP_CONST.ATTENTION:
                categoryStr = getString("eventCategoryAttention", lang);
                break;
            case LIFEMAP_CONST.LEVEL:
                categoryStr = getString("eventCategoryDriver", lang);
                break;
            case LIFEMAP_CONST.VET:
                categoryStr = getString("eventCategoryVet", lang);
                break;
            default:
                categoryStr = getString("eventCategoryNeedHelp", lang);
                break;
        }

        return categoryStr;
    }

//    public static Language getLanguageByCode(String languageCode) {
//        return supportedLanguages.stream().filter(x -> x.getCode().equals(languageCode)).findFirst().orElse(null);
//    }

    public static Language getLanguageByName(String languageName) {
        return supportedLanguages.stream().filter(x -> x.getName().equals(languageName)).findFirst().orElse(null);
    }

    public static Category getCategoryByName(String categoryName, String languageName) {
        Category cat;
        switch (languageName.toLowerCase()) {
            case "ru":
                cat = categories.stream().filter(x -> x.getNameRu().equals(categoryName)).findFirst().orElse(null);
                break;
            case "uz":
                cat = categories.stream().filter(x -> x.getNameUz().equals(categoryName)).findFirst().orElse(null);
                break;
            default:
                cat = categories.stream().filter(x -> x.getNameEn().equals(categoryName)).findFirst().orElse(null);
                break;
        }

        return cat;
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public static String getLanguageCodeByName(String language) {
        return supportedLanguages.stream().filter(x -> x.getName().equals(language))
                .map(Language::getCode).findFirst().orElse(null);
    }

    public static Integer getCategoryCodeByLang(String language) {
        return categories.stream().filter(x -> x.getNameEn().equals(language))
                .map(Category::getCode).findFirst().orElse(null);
    }

    private static class Utf8ResourceBundle extends ResourceBundle {

        private static final String BUNDLE_EXTENSION = "properties";
        private static final String CHARSET = "UTF-8";
        private static final Control UTF8_CONTROL = new UTF8Control();

        Utf8ResourceBundle(String bundleName, Locale locale) {
            setParent(ResourceBundle.getBundle(bundleName, locale, UTF8_CONTROL));
        }

        @Override
        protected Object handleGetObject(String key) {
            return parent.getObject(key);
        }

        @Override
        public Enumeration<String> getKeys() {
            return parent.getKeys();
        }

        private static class UTF8Control extends Control {
            public ResourceBundle newBundle
                    (String baseName, Locale locale, String format, ClassLoader loader, boolean reload)
                    throws IllegalAccessException, InstantiationException, IOException {
                String bundleName = toBundleName(baseName, locale);
                String resourceName = toResourceName(bundleName, BUNDLE_EXTENSION);
                ResourceBundle bundle = null;
                InputStream stream = null;
                if (reload) {
                    URL url = loader.getResource(resourceName);
                    if (url != null) {
                        URLConnection connection = url.openConnection();
                        if (connection != null) {
                            connection.setUseCaches(false);
                            stream = connection.getInputStream();
                        }
                    }
                } else {
                    stream = loader.getResourceAsStream(resourceName);
                }
                if (stream != null) {
                    try {
                        bundle = new PropertyResourceBundle(new InputStreamReader(stream, CHARSET));
                    } finally {
                        stream.close();
                    }
                }
                return bundle;
            }
        }
    }
}
