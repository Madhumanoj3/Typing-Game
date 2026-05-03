package util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class PasswordUtil {

    private PasswordUtil() { /* static utility */ }

    /** Returns a hex-encoded SHA-256 hash of the given plain-text password. */
    public static String hash(String plain) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encoded = digest.digest(plain.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : encoded) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    /** Checks that a plain-text password matches a stored hash. */
    public static boolean verify(String plain, String storedHash) {
        return hash(plain).equals(storedHash);
    }

    /**
     * Returns a strength score 0-3:
     *   0 = too short / blank
     *   1 = weak  (< 8 chars)
     *   2 = medium (8+ chars, mixed case or digits)
     *   3 = strong (8+ chars, uppercase + lowercase + digit + symbol)
     */
    public static int strength(String password) {
        if (password == null || password.length() < 6)  return 0;
        if (password.length() < 8)                       return 1;

        boolean upper  = password.chars().anyMatch(Character::isUpperCase);
        boolean lower  = password.chars().anyMatch(Character::isLowerCase);
        boolean digit  = password.chars().anyMatch(Character::isDigit);
        boolean symbol = password.chars().anyMatch(c -> !Character.isLetterOrDigit(c));

        int score = 0;
        if (upper && lower) score++;
        if (digit)          score++;
        if (symbol)         score++;

        return Math.min(3, score + 1);   // base 1 for length, up to 3
    }
}
