package vn.simtim.api.promotion.domain;

public class PromotionConflictException extends RuntimeException {
    public PromotionConflictException(String message) {
        super(message);
    }
}
