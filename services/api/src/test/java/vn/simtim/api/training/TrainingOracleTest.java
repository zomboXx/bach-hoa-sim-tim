package vn.simtim.api.training;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import vn.simtim.api.training.domain.TrainingOracle;

class TrainingOracleTest {
    @Test
    void accuracyThresholdIsConsistentWithTheApprovedScenarioRule() {
        assertThat(TrainingOracle.complete(8, 0))
                .extracting(TrainingOracle.Result::totalScore, TrainingOracle.Result::passed,
                        TrainingOracle.Result::grade)
                .containsExactly(100, true, "EXCELLENT");
        assertThat(TrainingOracle.complete(8, 2))
                .extracting(TrainingOracle.Result::totalScore, TrainingOracle.Result::passed,
                        TrainingOracle.Result::grade)
                .containsExactly(80, true, "SATISFACTORY");
        assertThat(TrainingOracle.complete(8, 3))
                .extracting(TrainingOracle.Result::totalScore, TrainingOracle.Result::passed,
                        TrainingOracle.Result::grade)
                .containsExactly(73, false, "NEEDS_PRACTICE");
    }
}
