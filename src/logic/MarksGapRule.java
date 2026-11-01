package logic;

import java.util.Optional;

/** Flags a subject whose score sits a wide margin below the student's own average. */
public class MarksGapRule implements GapRule {

    /** Percentage points below the student's own average that count as a gap. */
    public static final double MARGIN = 20.0;

    @Override
    public Optional<Flag> check(StudentSubject s) {
        if (s.scorePct() == null) return Optional.empty();
        double gap = s.overallScorePct() - s.scorePct();
        if (gap < MARGIN) return Optional.empty();
        return Optional.of(new Flag("marks", String.format(
            "Scoring %.0f%% here against %.0f%% overall (%.0f below)",
            s.scorePct(), s.overallScorePct(), gap)));
    }

    @Override
    public String name() {
        return "Marks gap";
    }
}
