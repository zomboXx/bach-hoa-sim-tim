package vn.simtim.api.sale;

import static org.assertj.core.api.Assertions.*;
import org.junit.jupiter.api.Test;
import vn.simtim.api.sale.application.VietQr;

class VietQrTest {
    @Test void matchesPublishedNapAsAccountTransferVector() {
        // Public example in xuannghia/vietnam-qr-pay; no beneficiary from project configuration.
        assertThat(VietQr.encode("970416","257678859",1000,"Chuyen tien"))
                .isEqualTo("00020101021238530010A0000007270123000697041601092576788590208QRIBFTTA5303704540410005802VN62150811Chuyen tien6304BBB8");
    }
    @Test void refusesUnencodableAmountsAndReferences() {
        assertThatIllegalArgumentException().isThrownBy(()->VietQr.encode("970436","1234567890",0,"ST123"));
        assertThatIllegalArgumentException().isThrownBy(()->VietQr.encode("970436","1234567890",10,"nội dung"));
    }
}
