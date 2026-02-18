/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.benchmarks.scripts;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the compare-results.sh script.
 */
class CompareResultsTest {

    private static final String SCRIPT_NAME = "compare-results.sh";
    private static final Path BASELINE_FIXTURE = Paths.get("src/test/resources/omb-result-baseline.json").toAbsolutePath();
    private static final Path PROXY_FIXTURE = Paths.get("src/test/resources/omb-result-proxy.json").toAbsolutePath();

    @Test
    void helpExitsSuccessfullyAndShowsUsage() {
        ScriptUtils.ScriptResult result = ScriptUtils.executeScript(SCRIPT_NAME, "--help");

        assertThat(result.exitCode())
                .as("--help should exit with code 0")
                .isZero();
        assertThat(result.output())
                .as("--help should show usage information")
                .contains("Usage");
    }

    @Test
    void noArgsExitsWithNonZero() {
        ScriptUtils.ScriptResult result = ScriptUtils.executeScript(SCRIPT_NAME);

        assertThat(result.exitCode())
                .as("No arguments should exit with non-zero code")
                .isNotZero();
        assertThat(result.output())
                .as("No arguments should still show usage")
                .contains("Usage");
    }

    @Nested
    class TwoFileComparison {

        private static ScriptUtils.ScriptResult result;

        @BeforeAll
        static void runComparison() {
            result = ScriptUtils.executeScript(SCRIPT_NAME,
                    BASELINE_FIXTURE.toString(), PROXY_FIXTURE.toString());
        }

        @Test
        void exitSuccessfully() {
            assertThat(result.exitCode())
                    .as("Comparison should succeed, output:\n%s", result.output())
                    .isZero();
        }

        @Test
        void outputContainsPublishLatencyHeader() {
            assertThat(result.output())
                    .as("Output should contain publish latency section header")
                    .contains("Publish Latency");
        }

        @Test
        void outputContainsAvgLatencyWithCorrectValues() {
            assertThat(result.output())
                    .as("Output should contain Avg row with baseline 5.12 and candidate 6.34")
                    .containsPattern("Avg\\s+5\\.12\\s+6\\.34");
        }

        @Test
        void outputContainsP99LatencyWithCorrectValues() {
            assertThat(result.output())
                    .as("Output should contain p99 row with baseline 25.60 and candidate 29.10")
                    .containsPattern("p99\\s+25\\.60\\s+29\\.10");
        }

        @Test
        void outputContainsEndToEndLatencyHeader() {
            assertThat(result.output())
                    .as("Output should contain end-to-end latency section header")
                    .contains("End-to-End Latency");
        }

        @Test
        void outputContainsEndToEndAvgLatencyWithCorrectValues() {
            // Baseline endToEndLatencyAvg: [8.50, 9.20, 8.80] → avg = 8.83
            // Proxy endToEndLatencyAvg: [10.20, 10.80, 10.50] → avg = 10.50
            assertThat(result.output())
                    .as("Output should contain end-to-end Avg row with baseline ~8.83 and candidate ~10.50")
                    .containsPattern("Avg\\s+8\\.8[0-9]\\s+10\\.50");
        }

        @Test
        void outputContainsEndToEndP99LatencyWithCorrectValues() {
            // Baseline endToEndLatency99pct: [35.40, 36.80, 36.10] → avg = 36.10
            // Proxy endToEndLatency99pct: [42.60, 44.00, 43.30] → avg = 43.30
            assertThat(result.output())
                    .as("Output should contain end-to-end p99 row with baseline ~36.10 and candidate ~43.30")
                    .containsPattern("p99\\s+36\\.10\\s+43\\.30");
        }

        @Test
        void outputContainsThroughputHeader() {
            assertThat(result.output())
                    .as("Output should contain throughput section header")
                    .contains("Throughput");
        }

        @Test
        void outputContainsPublishRateWithCorrectValues() {
            // Baseline publishRate: [50000.0, 49800.0, 50200.0] → avg = 50000.00
            // Proxy publishRate: [49500.0, 49200.0, 49700.0] → avg = 49466.67
            assertThat(result.output())
                    .as("Output should contain Publish Rate row with baseline ~50000 and candidate ~49467")
                    .containsPattern("Publish Rate\\s+50000\\.00\\s+49466\\.6[0-9]");
        }

        @Test
        void outputContainsConsumeRateWithCorrectValues() {
            // Baseline consumeRate: [49900.0, 50100.0, 50000.0] → avg = 50000.00
            // Proxy consumeRate: [49400.0, 49600.0, 49500.0] → avg = 49500.00
            assertThat(result.output())
                    .as("Output should contain Consume Rate row with baseline ~50000 and candidate ~49500")
                    .containsPattern("Consume Rate\\s+50000\\.00\\s+49500\\.00");
        }
    }
}
