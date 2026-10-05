package com.themainthread.contributioncity.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

final class TestProcesses {
    private TestProcesses() {
    }

    static Result run(Path directory, Map<String, String> environment, List<String> command)
            throws IOException, InterruptedException {
        Path log = Files.createTempFile(directory, "process-", ".log");
        try {
            ProcessBuilder builder = new ProcessBuilder(command).directory(directory.toFile())
                    .redirectErrorStream(true).redirectOutput(log.toFile());
            // Keep local Git checks independent of the caller's repository and global configuration.
            builder.environment().keySet().removeIf(key -> key.startsWith("GIT_") || key.startsWith("INPUT_")
                    || key.startsWith("GITHUB_") || key.equals("JSON_INPUTS"));
            builder.environment().putAll(environment);
            Process process = builder.start();
            try {
                assertTrue(process.waitFor(90, TimeUnit.SECONDS), "Process timed out: " + command.getFirst());
                return new Result(process.exitValue(), Files.readString(log));
            } finally {
                if (process.isAlive()) {
                    process.descendants().forEach(ProcessHandle::destroyForcibly);
                    process.destroyForcibly();
                    process.waitFor(5, TimeUnit.SECONDS);
                }
            }
        } finally {
            Files.deleteIfExists(log);
        }
    }

    record Result(int exitCode, String output) {
        String requireSuccess() {
            assertEquals(0, exitCode, output);
            return output.strip();
        }
    }
}
