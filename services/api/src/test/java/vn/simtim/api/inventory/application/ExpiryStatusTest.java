package vn.simtim.api.inventory.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

class ExpiryStatusTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 2);

    @ParameterizedTest
    @MethodSource("expiryCases")
    void classifiesBoundaryDates(LocalDate expiryDate, ExpiryStatus expected) {
        assertThat(ExpiryStatus.classify(expiryDate, TODAY)).isEqualTo(expected);
    }

    static Stream<Arguments> expiryCases() {
        return Stream.of(
                Arguments.of(TODAY.minusDays(1), ExpiryStatus.EXPIRED),
                Arguments.of(TODAY, ExpiryStatus.NEAR_EXPIRY),
                Arguments.of(TODAY.plusDays(7), ExpiryStatus.NEAR_EXPIRY),
                Arguments.of(TODAY.plusDays(8), ExpiryStatus.VALID),
                Arguments.of(null, ExpiryStatus.NO_EXPIRY));
    }
}
