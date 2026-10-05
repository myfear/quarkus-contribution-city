package com.themainthread.contributioncity.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PublishingIT {
    @TempDir
    Path directory;

    @Test
    void publishesOnlyChangedImagesAndPreservesTheProfileCheckout() throws Exception {
        Path remote = directory.resolve("remote.git");
        Path checkout = directory.resolve("profile");
        git(directory, "init", "--quiet", "--bare", remote.toString());
        git(directory, "init", "--quiet", "--initial-branch=main", checkout.toString());
        git(checkout, "remote", "add", "origin", remote.toString());
        git(checkout, "config", "user.name", "Fixture");
        git(checkout, "config", "user.email", "fixture@example.invalid");
        Files.writeString(checkout.resolve("README.md"), "Profile fixture\n");
        git(checkout, "add", "README.md");
        git(checkout, "commit", "--quiet", "-m", "Profile fixture");
        String original = git(checkout, "rev-parse", "HEAD");
        String originalBranch = git(checkout, "branch", "--show-current");
        Files.writeString(checkout.resolve("contribution-city.svg"), "<svg/>\n");
        publish(checkout);
        String first = git(remote, "rev-parse", "refs/heads/output");
        assertEquals("<svg/>", git(remote, "show", "output:contribution-city.svg"));

        assertTrue(publish(checkout).contains("Contribution city is unchanged."));
        assertEquals(first, git(remote, "rev-parse", "refs/heads/output"));

        Files.writeString(checkout.resolve("contribution-city.svg"), "<svg><title>Changed</title></svg>\n");
        publish(checkout);
        assertNotEquals(first, git(remote, "rev-parse", "refs/heads/output"));
        assertEquals("<svg><title>Changed</title></svg>", git(remote, "show", "output:contribution-city.svg"));
        assertEquals("2", git(remote, "rev-list", "--count", "refs/heads/output"));
        assertEquals(original, git(checkout, "rev-parse", "HEAD"));
        assertEquals(originalBranch, git(checkout, "branch", "--show-current"));
        assertEquals("Profile fixture\n", Files.readString(checkout.resolve("README.md")));
    }

    private String publish(Path checkout) throws Exception {
        Path script = Path.of(System.getProperty("city.test.project-directory"), "examples", "publish-output.sh");
        Map<String, String> environment = new java.util.HashMap<>(gitEnvironment());
        environment.put("GITHUB_WORKSPACE", checkout.toString());
        return TestProcesses.run(checkout, environment, List.of("bash", script.toString())).requireSuccess();
    }

    private String git(Path workingDirectory, String... arguments) throws Exception {
        List<String> command = new ArrayList<>(List.of("git", "-C", workingDirectory.toString()));
        command.addAll(List.of(arguments));
        return TestProcesses.run(workingDirectory, gitEnvironment(), command).requireSuccess();
    }

    private Map<String, String> gitEnvironment() throws Exception {
        Path config = Files.writeString(directory.resolve("empty-git-config"), "");
        return Map.of("GIT_CONFIG_GLOBAL", config.toString(), "GIT_CONFIG_NOSYSTEM", "1", "GIT_TERMINAL_PROMPT", "0");
    }
}
