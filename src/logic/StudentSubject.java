package logic;

/**
 * One student's standing in one subject, set against their own baseline across
 * every subject. The GapRules read this. Percentages are 0..100; a score or
 * attendance figure is null when there's no data for it yet.
 */
public record StudentSubject(
        String subject,
        Double scorePct,             // the student's average score in this subject
        double overallScorePct,      // their average score across every subject
        Double attendancePct,        // their attendance in this subject
        double overallAttendancePct  // their average attendance across subjects
) {}
