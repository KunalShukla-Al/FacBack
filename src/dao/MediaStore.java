package dao;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Copies a reporter's file into the app's media folder under a random name.
 * The database keeps only the copy's path, so the original can move or be deleted.
 */
public final class MediaStore {

    static final long MAX_BYTES = 25L * 1024 * 1024;
    static final Set<String> TYPES = Set.of("jpg", "jpeg", "png", "mp4", "mov", "webm");
    static final Path ROOT = Path.of("data", "media");

    private MediaStore() {}

    /** Removes a copy left behind when its report wasn't saved. */
    public static void discard(String storedPath) {
        try {
            Files.deleteIfExists(Path.of(storedPath));
        } catch (IOException e) {
            System.err.println("Couldn't remove " + storedPath + ": " + e.getMessage());
        }
    }

    /**
     * @return the copy's path, relative to the app folder
     * @throws ReportException when the file is too large, the wrong type, or can't be copied.
     */
    public static String store(Path source) throws ReportException {
        String name = source.getFileName().toString();
        int dot = name.lastIndexOf('.');
        String ext = dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
        if (!TYPES.contains(ext))
            throw new ReportException("Use a photo (jpg, png) or a short video (mp4, mov, webm).");
        try {
            if (Files.size(source) > MAX_BYTES)
                throw new ReportException("The file is over 25 MB.");
            Files.createDirectories(ROOT);
            Path target = ROOT.resolve(UUID.randomUUID() + "." + ext);
            Files.copy(source, target);
            return target.toString();
        } catch (IOException e) {
            throw new ReportException("Couldn't copy the file: " + e.getMessage());
        }
    }
}
