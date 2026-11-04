package dao;

/** Raised when a misconduct report can't be filed as given — for now, one carrying no media. */
public class ReportException extends Exception {
    public ReportException(String message) {
        super(message);
    }
}
