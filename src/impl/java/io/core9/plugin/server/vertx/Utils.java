package io.core9.plugin.server.vertx;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;


public class Utils {
	
    private static final String BASE64ALPHA = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

	/**
     * Signs a String value with a given MAC
     */
    public static String sign(String val, Mac mac) {
        mac.reset();
        return val + "." + base64(mac.doFinal(val.getBytes()));
    }

    /**
     * Returns the original value is the signature is correct. Null otherwise.
     */
    public static String unsign(String val, Mac mac) {
        int idx = val.lastIndexOf('.');

        if (idx == -1) {
            return null;
        }

        String str = val.substring(0, idx);
        if (val.equals(sign(str, mac))) {
            return str;
        }
        return null;
    }
    
    private static byte[] zeroPad(final int length, final byte[] bytes) {
        final byte[] padded = new byte[length];
        System.arraycopy(bytes, 0, padded, 0, bytes.length);
        return padded;
    }
    
    public static String base64(final byte[] stringArray) {

        final StringBuilder encoded = new StringBuilder();

        // determine how many padding bytes to add to the output
        final int paddingCount = (3 - (stringArray.length % 3)) % 3;
        // add any necessary padding to the input
        final byte[] paddedArray = zeroPad(stringArray.length + paddingCount, stringArray);
        // process 3 bytes at a time, churning out 4 output bytes
        for (int i = 0; i < paddedArray.length; i += 3) {
            final int j = ((paddedArray[i] & 0xff) << 16) +
                    ((paddedArray[i + 1] & 0xff) << 8) +
                    (paddedArray[i + 2] & 0xff);

            encoded.append(BASE64ALPHA.charAt((j >> 18) & 0x3f));
            encoded.append(BASE64ALPHA.charAt((j >> 12) & 0x3f));
            encoded.append(BASE64ALPHA.charAt((j >> 6) & 0x3f));
            encoded.append(BASE64ALPHA.charAt(j & 0x3f));
        }

        encoded.setLength(encoded.length() - paddingCount);
        return encoded.toString();
    }
    
    /**
     * Creates a new HmacSHA256 Message Authentication Code
     * @param secret The secret key used to create signatures
     * @return Mac implementation
     */
    public static Mac newHmacSHA256(String secret) {
        try {
            Mac hmacSHA256 = Mac.getInstance("HmacSHA256");
            hmacSHA256.init(new SecretKeySpec(secret.getBytes(), hmacSHA256.getAlgorithm()));
            return hmacSHA256;
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }
}
