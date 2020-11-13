package uz.smartgeo.lifemap.telegram.utils;

public class Language {
    private String code;
    private String name;
    private String emoji;

    public Language(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmoji() {
        return emoji;
    }

    public void setEmoji(String emoji) {
        this.emoji = emoji;
    }

    @Override
    public String toString() {
        if (emoji == null || emoji.isEmpty()) {
            return name;
        } else {
            return emoji + " " + name;
        }
    }
}
