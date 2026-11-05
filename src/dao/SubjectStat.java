package dao;

/**
 * One subject's numbers for one student: the student's own average score and
 * attendance, and the class average and size behind the Context Score. A score
 * or attendance figure is null when there's no data for it yet. Percentages 0..100.
 */
public record SubjectStat(
        int courseId,
        String code,
        String name,
        Double studentScorePct,
        Double classScorePct,
        int classSize,
        Double attendancePct
) {}
