public class FeedbackCalc {

    static double score(double clarity, double punctuality, double doubtSolving, double overall) {
        return (clarity + punctuality + doubtSolving + overall) / 4;
    }

    static String band(double score) {
        if (score >= 4.0) return "Excellent";
        else if (score >= 3.0) return "Good";
        else if (score >= 2.0) return "Average";
        else return "Needs improvement";
    }
}
