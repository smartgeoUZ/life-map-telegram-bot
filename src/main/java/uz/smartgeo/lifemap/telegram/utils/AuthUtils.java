package uz.smartgeo.lifemap.telegram.utils;

public class AuthUtils {

    public static boolean tokenIsValid(String token) {
        boolean isValid = false;

        if (token != null) {
            String tokenBase = token.substring(7);
            if (tokenBase.length() == 64) {
                isValid = true;
            }
        }
        return isValid;
    }

    public static String decrypt(String encrypted) {
        try {
//            IvParameterSpec iv = new IvParameterSpec(initVector.getBytes("UTF-8"));
//            SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");
//
//            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
//            cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);
//            byte[] original = cipher.doFinal(Base64.getDecoder().decode(encrypted));
//
//            return new String(original);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }
}
