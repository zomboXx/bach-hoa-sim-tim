package vn.simtim.api.training.domain;

/** Pure scoring rule shared by every training scenario version. */
public final class TrainingOracle {
    private TrainingOracle() {}

    public record Result(int totalScore, boolean passed, String grade, int errorCount) {}

    public static Result complete(int correctActions, int errors) {
        if (correctActions <= 0 || errors < 0)
            throw new IllegalArgumentException("correctActions must be positive and errors nonnegative");
        int score = Math.round((100f * correctActions) / (correctActions + errors));
        boolean passed = score >= 80;
        String grade = passed && score >= 95 ? "EXCELLENT"
                : passed ? "SATISFACTORY" : "NEEDS_PRACTICE";
        return new Result(score, passed, grade, errors);
    }
}
