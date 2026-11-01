package logic;

import java.util.Optional;

/**
 * One check the FA dashboard runs on a (student, subject) cell.
 *
 * A rule compares the subject against the student's OWN baseline, so it catches
 * "fine everywhere else, slipping here" rather than flagging a student who is
 * simply weak across the board. That is the project's point: the same student
 * can look different from one teacher to the next. A rule returns the Flag to
 * raise, or empty when the cell looks normal.
 */
public interface GapRule {

    Optional<Flag> check(StudentSubject s);

    /** A short name for the dashboard and the outcome log. */
    String name();
}
