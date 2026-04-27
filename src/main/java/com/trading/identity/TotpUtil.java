package com.trading.identity;

import java.nio.ByteBuffer;
import java.time.Instant;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class TotpUtil {

    private TotpUtil() {}

    public static boolean validate(String code, String base32Secret) {
        if (code == null || code.length() != 6) {
            return false;
        }
        long timestep = Instant.now().getEpochSecond() / 30;
        for (int i = -1; i <= 1; i++) {
            if (generateCode(base32Secret.getBytes(), timestep + i).equals(code)) {
                return true;
            }
        }
        return false;
    }

    private static String generateCode(byte[] key, long timestep) {
        try {
            byte[] timeBytes = ByteBuffer.allocate(8).putLong(timestep).array();
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] hash = mac.doFinal(timeBytes);
            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7f) << 24)
                    | ((hash[offset + 1] & 0xff) << 16)
                    | ((hash[offset + 2] & 0xff) << 8)
                    | (hash[offset + 3] & 0xff);
            return String.format("%06d", binary % 1_000_000);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to generate TOTP", ex);
        }
    }
}
