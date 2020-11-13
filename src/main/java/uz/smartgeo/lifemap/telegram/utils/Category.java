package uz.smartgeo.lifemap.telegram.utils;

public class Category {
    private int code;
    private String nameCode;
    private String nameEn;
    private String nameRu;
    private String nameUz;
    private String emoji;

    public Category(int code, String nameCode, String nameEn, String nameRu, String nameUz) {
        this.code = code;
        this.nameCode = nameCode;
        this.nameEn = nameEn;
        this.nameRu = nameRu;
        this.nameUz = nameUz;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getNameCode() {
        return nameCode;
    }

    public String getNameEn() {
        return nameEn;
    }

    public String getNameRu() {
        return nameRu;
    }

    public String getNameUz() {
        return nameUz;
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
            return nameEn;
        } else {
            return emoji + " " + nameEn;
        }
    }
}
