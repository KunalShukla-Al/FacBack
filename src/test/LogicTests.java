package test;

import java.util.Optional;

import logic.AttendanceGapRule;
import logic.ContextScore;
import logic.Flag;
import logic.MarksGapRule;
import logic.NotEnoughDataException;
import logic.StudentSubject;

/**
 * Unit tests for the pure {@code logic} package — no database, no UI. This is
 * the heart of the PBL-2 Faculty Advisor dashboard: the Context Score bands and
 * the two gap rules that read a subject against the student's OWN baseline.
 *
 * No JUnit on the classpath, so this is a tiny self-contained harness. Run it:
 * <pre>
 *   javac -d bin src/logic/*.java src/test/LogicTests.java
 *   java  -cp bin test.LogicTests
 * </pre>
 */
public class LogicTests {

    static int passed = 0, failed = 0;

    public static void main(String[] args) {
        run("band sits at each boundary", () -> {
            eq(5, ContextScore.band(110, 100));   // 110% and up
            eq(4, ContextScore.band(109, 100));
            eq(4, ContextScore.band(90, 100));    // 90..109
            eq(3, ContextScore.band(89, 100));
            eq(3, ContextScore.band(70, 100));    // 70..89
            eq(2, ContextScore.band(69, 100));
            eq(2, ContextScore.band(50, 100));    // 50..69
            eq(1, ContextScore.band(49, 100));    // below 50
        });

        run("band rejects a zero class average", () -> {
            try {
                ContextScore.band(80, 0);
                throw new AssertionError("expected NotEnoughDataException");
            } catch (NotEnoughDataException expected) {
                // the class average is unusable; a band would be a lie
            }
        });

        run("score is hidden for a class too thin to average", () -> {
            ok(!ContextScore.visible(ContextScore.MIN_CLASS - 1));
            ok(ContextScore.visible(ContextScore.MIN_CLASS));
        });

        run("marks gap flags a wide shortfall below the student's own average", () -> {
            Optional<Flag> f = new MarksGapRule().check(
                    new StudentSubject("OS", 55.0, 80.0, null, 0));   // 25 below own 80
            ok(f.isPresent());
            eq("marks", f.get().kind());
        });

        run("marks gap ignores a small dip and missing data", () -> {
            ok(new MarksGapRule().check(new StudentSubject("OS", 65.0, 80.0, null, 0)).isEmpty()); // 15 < margin
            ok(new MarksGapRule().check(new StudentSubject("OS", 60.0, 80.0, null, 0)).isPresent()); // exactly 20
            ok(new MarksGapRule().check(new StudentSubject("OS", null, 80.0, null, 0)).isEmpty());  // no score
        });

        run("attendance gap flags a wide shortfall below the student's own average", () -> {
            Optional<Flag> f = new AttendanceGapRule().check(
                    new StudentSubject("OS", null, 0, 60.0, 85.0));   // 25 below own 85
            ok(f.isPresent());
            eq("attendance", f.get().kind());
        });

        run("attendance gap ignores a small dip and missing data", () -> {
            ok(new AttendanceGapRule().check(new StudentSubject("OS", null, 0, 72.0, 85.0)).isEmpty()); // 13 < margin
            ok(new AttendanceGapRule().check(new StudentSubject("OS", null, 0, null, 85.0)).isEmpty()); // no attendance
        });

        System.out.printf("%n%d passed, %d failed%n", passed, failed);
        if (failed > 0) System.exit(1);
    }

    // ---- tiny harness ----

    interface Check {
        void run() throws Exception;
    }

    static void run(String name, Check c) {
        try {
            c.run();
            passed++;
            System.out.println("ok: " + name);
        } catch (Throwable t) {
            failed++;
            System.out.println("FAIL: " + name + " — " + t.getMessage());
        }
    }

    static void eq(Object expected, Object got) {
        if (!expected.equals(got)) throw new AssertionError("expected " + expected + ", got " + got);
    }

    static void ok(boolean cond) {
        if (!cond) throw new AssertionError("expected true");
    }
}
