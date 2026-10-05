package com.themainthread.contributioncity.action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkiverse.githubaction.Commands;
import io.quarkiverse.githubaction.CommandsInitializer;
import io.quarkiverse.githubaction.Context;
import io.quarkiverse.githubaction.ContextInitializer;
import io.quarkiverse.githubaction.Inputs;
import io.quarkiverse.githubaction.InputsInitializer;
import io.quarkiverse.githubaction.runtime.CommandsImpl;
import io.quarkiverse.githubaction.runtime.github.EnvFiles;
import io.quarkiverse.githubaction.testing.DefaultTestContext;
import io.quarkiverse.githubaction.testing.DefaultTestInputs;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.junit.main.LaunchResult;
import io.quarkus.test.junit.main.QuarkusMainLauncher;
import io.quarkus.test.junit.main.QuarkusMainTest;

import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Singleton;
import jakarta.json.Json;
import jakarta.json.JsonObject;

@QuarkusMainTest
@TestProfile(ContributionCityActionTest.Profile.class)
@QuarkusTestResource(FixtureGraphQLServer.class)
class ContributionCityActionTest {
    @BeforeEach
    @AfterEach
    void cleanup() throws IOException {
        System.clearProperty("city.test.username");
        Files.deleteIfExists(Path.of("contribution-city.svg"));
    }

    @Test
    void writesFileOutputsAndSummaryWithOneRequestPerRun(QuarkusMainLauncher launcher) throws Exception {
        long before = requestCount();
        LaunchResult result = launcher.launch();
        assertEquals(0, result.exitCode());
        String first = Files.readString(Path.of("contribution-city.svg"));
        assertTrue(first.startsWith("<svg"));
        assertEquals("svg-path=contribution-city.svg\ntotal-contributions=44\n",
                Files.readString(Path.of("target/wiring-output.txt")).replace("\r\n", "\n"));
        assertTrue(Files.readString(Path.of("target/wiring-summary.md")).contains("4 weeks rendered"));
        assertEquals(before + 1, requestCount());
        JsonObject request = Json
                .createReader(new StringReader(Files.readAllLines(Path.of("target/wiring-requests.jsonl")).getLast()))
                .readObject();
        assertEquals("fixture-user", request.getJsonObject("variables").getString("login"));
        assertFalse(request.getString("query").contains("rateLimit"));
        assertEquals(0, launcher.launch().exitCode());
        assertEquals(first, Files.readString(Path.of("contribution-city.svg")));
        assertEquals(before + 2, requestCount());
    }

    @Test
    void invalidInputFailsBeforeAnyRequest(QuarkusMainLauncher launcher) throws Exception {
        long before = requestCount();
        System.setProperty("city.test.username", "bad/user");
        assertTrue(launcher.launch().exitCode() != 0);
        assertEquals(before, requestCount());
        assertFalse(Files.exists(Path.of("contribution-city.svg")));
        assertEquals("", Files.readString(Path.of("target/wiring-output.txt")));
    }

    @Test
    void writeFailureDoesNotExposeSuccessOutputs(QuarkusMainLauncher launcher) throws Exception {
        Files.createDirectory(Path.of("contribution-city.svg"));
        assertTrue(launcher.launch().exitCode() != 0);
        assertEquals("", Files.readString(Path.of("target/wiring-output.txt")));
    }

    @Test
    void graphqlFailureDoesNotProduceFileOrOutputs(QuarkusMainLauncher launcher) throws Exception {
        long before = requestCount();
        System.setProperty("city.test.username", "missing-user");
        LaunchResult result = launcher.launch();
        assertTrue(result.exitCode() != 0);
        assertTrue((result.getOutput() + result.getErrorOutput()).contains("GitHub returned GraphQL errors"));
        assertEquals(before + 1, requestCount());
        assertFalse(Files.exists(Path.of("contribution-city.svg")));
        assertEquals("", Files.readString(Path.of("target/wiring-output.txt")));
    }

    private long requestCount() throws IOException {
        Path requests = Path.of("target/wiring-requests.jsonl");
        return Files.exists(requests) ? Files.readAllLines(requests).size() : 0;
    }

    public static class Profile implements QuarkusTestProfile {
        @Override
        public Set<Class<?>> getEnabledAlternatives() {
            return Set.of(TestInputs.class, TestCommands.class, TestContext.class);
        }
    }

    @Alternative
    @Singleton
    public static class TestInputs implements InputsInitializer {
        @Override
        public Inputs createInputs() {
            return new DefaultTestInputs(Map.of("username", System.getProperty("city.test.username", "fixture-user"),
                    "github-token", "fixture-token", "weeks", "4", "height", "3"));
        }
    }

    @Alternative
    @Singleton
    public static class TestCommands implements CommandsInitializer {
        @Override
        public Commands createCommands() {
            try {
                Files.writeString(Path.of("target/wiring-output.txt"), "");
                Files.writeString(Path.of("target/wiring-summary.md"), "");
                return new CommandsImpl(Map.of(EnvFiles.GITHUB_OUTPUT, "target/wiring-output.txt",
                        EnvFiles.GITHUB_STEP_SUMMARY, "target/wiring-summary.md"));
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    @Alternative
    @Singleton
    public static class TestContext implements ContextInitializer {
        @Override
        public Context createContext() {
            return new DefaultTestContext() {
                @Override
                public String getGithubGraphQLUrl() {
                    return ConfigProvider.getConfig().getValue("city.test.graphql.url", String.class);
                }
            };
        }
    }
}
