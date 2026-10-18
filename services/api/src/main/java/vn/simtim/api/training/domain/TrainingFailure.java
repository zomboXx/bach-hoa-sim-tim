package vn.simtim.api.training.domain;

public class TrainingFailure extends RuntimeException {
    private final String code;
    private final int status;

    public TrainingFailure(int status, String code, String message) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public String code() { return code; }
    public int status() { return status; }
}
