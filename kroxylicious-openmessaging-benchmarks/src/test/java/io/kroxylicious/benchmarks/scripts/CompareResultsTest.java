/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.benchmarks.scripts;

import java.nio.file.Path;
import java.nio.file.Paths;

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
        // When
        ScriptUtils.ScriptResult result = ScriptUtils.executeScript(SCRIPT_NAME, "--help");

        // Then
        assertThat(result.exitCode())
                .as("--help should exit with code 0")
                .isZero();
        assertThat(result.output())
                .as("--help should show usage information")
                .contains("Usage");
    }

    @Test
    void noArgsExitsWithNonZero() {
        // When
        ScriptUtils.ScriptResult result = ScriptUtils.executeScript(SCRIPT_NAME);

        // Then
        assertThat(result.exitCode())
                .as("No arguments should exit with non-zero code")
                .isNotZero();
        assertThat(result.output())
                .as("No arguments should still show usage")
                .contains("Usage");
    }

    @Test
    void comparesPublishLatencyBetweenTwoFiles() {
        // When
        ScriptUtils.ScriptResult result = ScriptUtils.executeScript(SCRIPT_NAME,
                BASELINE_FIXTURE.toString(), PROXY_FIXTURE.toString());

        // Then
        assertThat(result.exitCode())
                .as("Comparison should succeed, output:\n%s", result.output())
                .isZero();
        assertThat(result.output())
                .as("Output should contain publish latency header")
                .contains("Publish Latency");
        assertThat(result.output())
                .as("Output should contain Avg row with baseline value 5.12")
                .contains("Avg")
                .contains("5.12");
        assertThat(result.output())
                .as("Output should contain p99 row with baseline value 25.60")
                .contains("p99")
                .contains("25.60");
    }
}
