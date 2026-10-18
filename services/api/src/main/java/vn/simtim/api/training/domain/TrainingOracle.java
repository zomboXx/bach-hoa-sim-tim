package vn.simtim.api.training.domain;

/** Pure scoring rule shared by every training scenario version. */
public final class TrainingOracle {
    private TrainingOracle() {}

    public record Result(int totalScore, boolean passed, String grade, int errorCount) {}

    public static Result complete(int errors) {
        if (errors < 0) throw new IllegalArgumentException("errors must be nonnegative");
        int score = errors >= 10 ? 0 : 100 - errors * 10;
        String grade = errors == 0 ? "EXCELLENT"
                : errors <= 3 ? "SATISFACTORY" : "NEEDS_PRACTICE";
        return new Result(score, true, grade, errors);
    }
}
