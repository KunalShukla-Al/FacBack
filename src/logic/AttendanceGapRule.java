package logic;

import java.util.Optional;

/** Flags a subject the student attends far less than they attend overall. */
public class AttendanceGapRule implements GapRule {

    /** Percentage points below the student's own attendance that count as a gap. */
    public static final double MARGIN = 20.0;

    @Override
    public Optional<Flag> check(StudentSubject s) {
        if (s.attendancePct() == null) return Optional.empty();
        double gap = s.overallAttendancePct() - s.attendancePct();
        if (gap < MARGIN) return Optional.empty();
        return Optional.of(new Flag("attendance", String.format(
            "Attending %.0f%% here against %.0f%% overall (%.0f below)",
            s.attendancePct(), s.overallAttendancePct(), gap)));
    }

    @Override
    public String name() {
        return "Attendance gap";
    }
}
