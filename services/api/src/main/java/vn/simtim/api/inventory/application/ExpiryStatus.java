package vn.simtim.api.inventory.application;

import java.time.LocalDate;

/** Trạng thái hạn của lô tại ngày nghiệp vụ. */
public enum ExpiryStatus {
    EXPIRED,
    NEAR_EXPIRY,
    VALID,
    NO_EXPIRY;

    public static ExpiryStatus classify(LocalDate expiryDate, LocalDate businessDate) {
        if (expiryDate == null) {
            return NO_EXPIRY;
        }
        if (expiryDate.isBefore(businessDate)) {
            return EXPIRED;
        }
        if (!expiryDate.isAfter(businessDate.plusDays(7))) {
            return NEAR_EXPIRY;
        }
        return VALID;
    }
}
