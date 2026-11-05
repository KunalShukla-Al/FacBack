package logic;

/** A concern a {@link GapRule} raises about one student in one subject, shown to the FA only. */
public record Flag(String kind, String message) {}
