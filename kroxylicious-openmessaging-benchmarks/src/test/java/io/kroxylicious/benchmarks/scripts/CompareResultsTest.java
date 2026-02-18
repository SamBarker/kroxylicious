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
    class PublishLatencyComparison {

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
    }
}
