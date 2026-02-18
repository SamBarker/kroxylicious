/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.benchmarks.scripts;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the collect-results.sh script.
 */
class CollectResultsTest {

    private static final String SCRIPT_NAME = "collect-results.sh";
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

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
    }

    @Test
    void generateRunMetadataCreatesValidJson(@TempDir Path tempDir) throws IOException {
        ScriptUtils.ScriptResult result = ScriptUtils.executeScript(SCRIPT_NAME,
                "--generate-run-metadata", tempDir.toString());

        assertThat(result.exitCode())
                .as("--generate-run-metadata should succeed, output:\n%s", result.output())
                .isZero();

        Path metadataFile = tempDir.resolve("run-metadata.json");
        assertThat(metadataFile)
                .as("run-metadata.json should be created")
                .exists();

        JsonNode metadata = JSON_MAPPER.readTree(Files.readString(metadataFile));
        assertThat(metadata.has("gitCommit"))
                .as("Metadata should contain gitCommit field")
                .isTrue();
        assertThat(metadata.has("gitBranch"))
                .as("Metadata should contain gitBranch field")
                .isTrue();
        assertThat(metadata.has("timestamp"))
                .as("Metadata should contain timestamp field")
                .isTrue();
    }
}
