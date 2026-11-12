package logic;

/**
 * Stands in for the local vision model. Every report scores 0, so the queue
 * keeps its newest-first order until a real triage is wired in.
 */
public class StubTriage implements ReportTriage {

    @Override
    public double score(String mediaPath) {
        return 0;
    }
}
