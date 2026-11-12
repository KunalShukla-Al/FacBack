package logic;

/**
 * Scores a report's media so the teacher's queue can show likely cases first.
 * It orders the queue and never removes a report.
 */
public interface ReportTriage {

    /** Higher scores are reviewed first. */
    double score(String mediaPath);
}
