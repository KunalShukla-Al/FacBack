package logic;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Keyed hash of "this student reviewed this course", used for the 14-day
 * cooldown. The key comes from FACBACK_SECRET, or from data/review.key, which
 * is created on first use. Without the key, a stored hash can't be matched
 * back to an RA number. Changing the key restarts every cooldown.
 */
public final class ReviewHasher {

    private static final Path KEY_FILE = Path.of("data", "review.key");
    private static byte[] key;

    private ReviewHasher() {}

    public static synchronized String hash(String username, int courseId)
            throws IOException, GeneralSecurityException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key(), "HmacSHA256"));
        byte[] tag = mac.doFinal((username + ":" + courseId).getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(tag);
    }

    private static byte[] key() throws IOException {
        if (key != null) return key;
        String env = System.getenv("FACBACK_SECRET");
        if (env != null && !env.isBlank()) return key = env.getBytes(StandardCharsets.UTF_8);
        if (Files.exists(KEY_FILE))
            return key = Base64.getDecoder().decode(Files.readString(KEY_FILE).trim());
        byte[] fresh = new byte[32];
        new SecureRandom().nextBytes(fresh);
        Files.createDirectories(KEY_FILE.getParent());
        Files.writeString(KEY_FILE, Base64.getEncoder().encodeToString(fresh));
        return key = fresh;
    }
}
