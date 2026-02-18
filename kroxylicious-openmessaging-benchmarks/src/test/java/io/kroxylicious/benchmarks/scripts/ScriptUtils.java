/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.benchmarks.scripts;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.fail;

/**
 * Utility class for executing shell scripts under test.
 */
public class ScriptUtils {

    private static final Path SCRIPTS_DIR = getScriptsDirectory();
    private static final int PROCESS_TIMEOUT_SECONDS = 30;

    private static Path getScriptsDirectory() {
        String scriptsDirProperty = System.getProperty("scripts.directory");
        if (scriptsDirProperty != null) {
            return Paths.get(scriptsDirProperty);
        }
        return Paths.get("scripts").toAbsolutePath();
    }

    /**
     * Result of executing a script, containing exit code and output.
     */
    public record ScriptResult(int exitCode, String output) {}

    /**
     * Executes a script with the given arguments and returns the result.
     *
     * @param scriptName Name of the script in the scripts directory
     * @param args Arguments to pass to the script
     * @return ScriptResult containing exit code and combined stdout/stderr
     */
    public static ScriptResult executeScript(String scriptName, String... args) {
        List<String> command = new ArrayList<>();
        command.add(SCRIPTS_DIR.resolve(scriptName).toString());
        command.addAll(List.of(args));

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);

        try {
            Process process = pb.start();
            String output;

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                output = reader.lines().collect(Collectors.joining("\n"));
            }

            boolean finished = process.waitFor(PROCESS_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                fail("Script timed out (after %s): %s", Duration.ofSeconds(PROCESS_TIMEOUT_SECONDS), String.join(" ", command));
            }

            return new ScriptResult(process.exitValue(), output);
        }
        catch (IOException ioe) {
            fail("Failed to execute script: " + String.join(" ", command), ioe);
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("Failed to execute script: " + String.join(" ", command), e);
        }
        return null;
    }
}
