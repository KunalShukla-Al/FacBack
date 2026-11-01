package logic;

/** Raised when a Context Score can't be trusted: too small a class, or no class average. */
public class NotEnoughDataException extends Exception {
    public NotEnoughDataException(String message) {
        super(message);
    }
}
