/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.benchmarks.scripts;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

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

    /**
     * Extracts the data rows belonging to a named section from the script output.
     * A section starts with a header line containing the section name and ends
     * at the next blank line or end of output. Data rows are the lines after the
     * column header and separator lines.
     */
    private static List<String> extractSection(String output, String sectionName) {
        List<String> rows = new ArrayList<>();
        boolean inSection = false;
        int headerLinesSeen = 0;

        for (String line : output.split("\n")) {
            if (line.contains(sectionName)) {
                inSection = true;
                headerLinesSeen = 0;
                continue;
            }
            if (inSection) {
                if (line.isBlank()) {
                    break;
                }
                // Skip the column header ("Metric Baseline ...") and separator ("----")
                headerLinesSeen++;
                if (headerLinesSeen > 2) {
                    rows.add(line);
                }
            }
        }
        return rows;
    }

    /**
     * Finds the data row for a given metric label within a section's rows.
     * Matches by trimming the row and checking it starts with the exact label
     * followed by whitespace, so "p99" does not match a "p99.9" row.
     */
    private static String findRow(List<String> sectionRows, String metricLabel) {
        return sectionRows.stream()
                .filter(row -> row.trim().startsWith(metricLabel + " ") || row.trim().equals(metricLabel))
                .findFirst()
                .orElse(null);
    }

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
        private static List<String> publishLatencyRows;
        private static List<String> endToEndLatencyRows;
        private static List<String> throughputRows;

        @BeforeAll
        static void runComparison() {
            result = ScriptUtils.executeScript(SCRIPT_NAME,
                    BASELINE_FIXTURE.toString(), PROXY_FIXTURE.toString());
            publishLatencyRows = extractSection(result.output(), "Publish Latency");
            endToEndLatencyRows = extractSection(result.output(), "End-to-End Latency");
            throughputRows = extractSection(result.output(), "Throughput");
        }

        @Test
        void exitSuccessfully() {
            assertThat(result.exitCode())
                    .as("Comparison should succeed, output:\n%s", result.output())
                    .isZero();
        }

        @Test
        void outputContainsPublishLatencySection() {
            assertThat(publishLatencyRows)
                    .as("Publish Latency section should contain data rows")
                    .isNotEmpty();
        }

        @Test
        void publishLatencyAvgShowsCorrectValues() {
            String row = findRow(publishLatencyRows, "Avg");
            assertThat(row)
                    .as("Publish Latency should have an Avg row")
                    .isNotNull()
                    .contains("5.12", "6.34");
        }

        @Test
        void publishLatencyP99ShowsCorrectValues() {
            String row = findRow(publishLatencyRows, "p99");
            assertThat(row)
                    .as("Publish Latency should have a p99 row (not p99.9)")
                    .isNotNull()
                    .contains("25.60", "29.10");
        }

        @Test
        void outputContainsEndToEndLatencySection() {
            assertThat(endToEndLatencyRows)
                    .as("End-to-End Latency section should contain data rows")
                    .isNotEmpty();
        }

        @Test
        void endToEndLatencyAvgShowsCorrectValues() {
            // Baseline endToEndLatencyAvg: [8.50, 9.20, 8.80] → avg = 8.83
            // Proxy endToEndLatencyAvg: [10.20, 10.80, 10.50] → avg = 10.50
            String row = findRow(endToEndLatencyRows, "Avg");
            assertThat(row)
                    .as("End-to-End Latency should have an Avg row")
                    .isNotNull()
                    .contains("8.83", "10.50");
        }

        @Test
        void endToEndLatencyP99ShowsCorrectValues() {
            // Baseline endToEndLatency99pct: [35.40, 36.80, 36.10] → avg = 36.10
            // Proxy endToEndLatency99pct: [42.60, 44.00, 43.30] → avg = 43.30
            String row = findRow(endToEndLatencyRows, "p99");
            assertThat(row)
                    .as("End-to-End Latency should have a p99 row (not p99.9)")
                    .isNotNull()
                    .contains("36.10", "43.30");
        }

        @Test
        void outputContainsThroughputSection() {
            assertThat(throughputRows)
                    .as("Throughput section should contain data rows")
                    .isNotEmpty();
        }

        @Test
        void publishRateShowsCorrectValues() {
            // Baseline publishRate: [50000.0, 49800.0, 50200.0] → avg = 50000.00
            // Proxy publishRate: [49500.0, 49200.0, 49700.0] → avg = 49466.67
            String row = findRow(throughputRows, "Publish Rate");
            assertThat(row)
                    .as("Throughput should have a Publish Rate row")
                    .isNotNull()
                    .contains("50000.00", "49466.67");
        }

        @Test
        void consumeRateShowsCorrectValues() {
            // Baseline consumeRate: [49900.0, 50100.0, 50000.0] → avg = 50000.00
            // Proxy consumeRate: [49400.0, 49600.0, 49500.0] → avg = 49500.00
            String row = findRow(throughputRows, "Consume Rate");
            assertThat(row)
                    .as("Throughput should have a Consume Rate row")
                    .isNotNull()
                    .contains("50000.00", "49500.00");
        }
    }
}
