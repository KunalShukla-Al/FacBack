package logic;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Salted PBKDF2 hashes for stored passwords, in the form
 * {@code pbkdf2$iterations$salt$hash}. A value without that prefix is the
 * plain text an account was created with before hashing, and {@link #matches}
 * still checks it so the login can upgrade it.
 */
public final class Passwords {

    private static final String PREFIX = "pbkdf2$";
    private static final int ITERATIONS = 210_000;
    private static final int KEY_BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    private Passwords() {}

    public static String hash(String password) throws GeneralSecurityException {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return PREFIX + ITERATIONS + "$" + enc(salt) + "$" + enc(derive(password, salt, ITERATIONS));
    }

    public static boolean isHashed(String stored) {
        return stored.startsWith(PREFIX);
    }

    public static boolean matches(String password, String stored) throws GeneralSecurityException {
        if (!isHashed(stored)) return stored.equals(password);
        String[] parts = stored.split("\\$");
        byte[] actual = derive(password, dec(parts[2]), Integer.parseInt(parts[1]));
        return MessageDigest.isEqual(dec(parts[3]), actual);
    }

    private static byte[] derive(String password, byte[] salt, int iterations) throws GeneralSecurityException {
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, KEY_BITS);
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
    }

    private static String enc(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    private static byte[] dec(String text) {
        return Base64.getDecoder().decode(text);
    }
}
