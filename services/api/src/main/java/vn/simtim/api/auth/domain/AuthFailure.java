package vn.simtim.api.auth.domain;

public class AuthFailure extends RuntimeException {
    private final String code;

    public AuthFailure(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() { return code; }
}
