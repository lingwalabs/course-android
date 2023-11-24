package es.cursonoruego.util;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class RestSecurityHelper {

    private static final String SECRET = "****************";

    public static String getChecksum(String email) {
        Log.d(RestSecurityHelper.class.getName(), "getChecksum");
        String checksum = null;

        String input = email + SECRET;
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            byte[] md5AsBytes = messageDigest.digest(input.getBytes("UTF-8"));
            checksum = new BigInteger(1, md5AsBytes).toString(16);
        } catch (NoSuchAlgorithmException e) {
            Log.e(RestSecurityHelper.class.getName(), null, e);
        } catch (UnsupportedEncodingException e) {
            Log.e(RestSecurityHelper.class.getName(), null, e);
        }

        return checksum;
    }
}