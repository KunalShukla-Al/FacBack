package logic;

/**
 * A 1..5 reading of how a student's marks sit against their class in one subject
 * (their average divided by the class average).
 *
 * The FA sees it beside an anonymous review, so a harsh comment from a top
 * scorer reads differently from the same words by someone struggling; the
 * teacher never sees it. It's a number, not a label like "unfair", because a
 * number gives away less and never names what it implies.
 */
public final class ContextScore {

    /** Below this many students a class average is too thin to stand on. */
    public static final int MIN_CLASS = 10;

    private ContextScore() {}

    /** Hide the score for a class too small to average fairly. */
    public static boolean visible(int classSize) {
        return classSize >= MIN_CLASS;
    }

    /**
     * The band from the ratio of the student's average to the class average:
     * 5 at 110% and up, 4 at 90–109%, 3 at 70–89%, 2 at 50–69%, 1 below 50%.
     */
    public static int band(double studentAvg, double classAvg) throws NotEnoughDataException {
        if (classAvg <= 0) throw new NotEnoughDataException("No class average to compare against");
        double ratio = studentAvg / classAvg * 100;
        if (ratio >= 110) return 5;
        if (ratio >= 90) return 4;
        if (ratio >= 70) return 3;
        if (ratio >= 50) return 2;
        return 1;
    }
}
