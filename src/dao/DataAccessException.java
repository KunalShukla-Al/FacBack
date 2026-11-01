package dao;

/** Wraps a SQLException so the UI and the rules never import java.sql. */
public class DataAccessException extends RuntimeException {
    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
