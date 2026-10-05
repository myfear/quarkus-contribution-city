package com.themainthread.contributioncity.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.tools.ToolProvider;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ReleaseIT {
    @TempDir
    static Path compiled;
    @TempDir
    Path directory;

    @BeforeAll
    static void compileReleasePolicy() {
        assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, null, "--release", "21", "-d",
                compiled.toString(), project().resolve(".github/release/ReleaseVersion.java").toString()));
    }

    @ParameterizedTest
    @ValueSource(strings = { "v1.0.0", "v0.2.0-alpha", "v5.9.0-beta.3", "v1.2.3-rc.1", "v1.2.3-0",
            "v999999999999999999999.0.0" })
    void acceptsVersionTags(String tag) throws Exception {
        String plan = plan(tag, "").requireSuccess();
        assertTrue(plan.contains("prerelease=" + tag.contains("-")));
        assertTrue(plan.contains("latest=" + !tag.contains("-")));
        assertTrue(plan.contains("advance_major=" + !tag.contains("-")));
    }

    @ParameterizedTest
    @ValueSource(strings = { "1.0.0", "v1", "v1.2", "v01.2.3", "v1.02.3", "v1.2.03", "v1.0.0-beta.03",
            "v1.0.0-", "v1.0.0-beta..1", "v1.0.0+build.1", "v1.0.0\nlatest=true", "v1.0.0;echo bad" })
    void rejectsInvalidOrUnsupportedTags(String tag) throws Exception {
        assertNotEquals(0, plan(tag, "").exitCode());
        assertFalse(Files.exists(directory.resolve("plan")));
    }

    @Test
    void ordersVersionsNumericallyAndPreservesLatestForBackports() throws Exception {
        assertTrue(plan("v1.10.0", "v1.9.9\n").requireSuccess().contains("latest=true"));
        String backport = plan("v1.2.4", "v2.0.0\nv1.2.3\n").requireSuccess();
        assertTrue(backport.contains("latest=false"));
        assertTrue(backport.contains("advance_major=true"));
        String older = plan("v1.2.4", "v1.3.0\n").requireSuccess();
        assertTrue(older.contains("latest=false"));
        assertTrue(older.contains("advance_major=false"));
    }

    @Test
    void publishesStableReleaseAndRetriesWithoutMovingTheFullVersionTag() throws Exception {
        Path checkout = fixture("");
        publish(checkout, "v1.0.0").requireSuccess();
        Path remote = directory.resolve("remote.git");
        String tagObject = git(remote, "rev-parse", "v1.0.0");
        assertEquals("tag", git(remote, "cat-file", "-t", "v1.0.0"));
        assertEquals(git(checkout, "rev-parse", "HEAD"), git(remote, "rev-parse", "v1"));
        assertTrue(Files.readString(directory.resolve("calls")).contains("--latest=true"));
        publish(checkout, "v1.0.0").requireSuccess();
        assertEquals(tagObject, git(remote, "rev-parse", "v1.0.0"));
        assertEquals(1, Files.readAllLines(directory.resolve("calls")).stream().filter("create"::equals).count());

        Files.writeString(checkout.resolve("change"), "new source");
        git(checkout, "add", "change");
        git(checkout, "commit", "--quiet", "-m", "Next revision");
        git(checkout, "push", "--quiet", "origin", "main");
        assertNotEquals(0, publish(checkout, "v1.0.0").exitCode());
        assertEquals(tagObject, git(remote, "rev-parse", "v1.0.0"));
    }

    @Test
    void publishesPrereleaseWithoutMajorAliasOrLatest() throws Exception {
        Path checkout = fixture("");
        publish(checkout, "v1.0.0-beta.1").requireSuccess();
        String flags = Files.readString(directory.resolve("calls"));
        assertTrue(flags.contains("--prerelease"));
        assertTrue(flags.contains("--latest=false"));
        assertEquals("v1.0.0-beta.1", git(directory.resolve("remote.git"), "tag", "--list"));
    }

    @Test
    void backportAdvancesItsOwnMajorWhileKeepingGlobalLatest() throws Exception {
        Path checkout = fixture("v2.0.0\nv1.2.3\n");
        publish(checkout, "v1.2.4").requireSuccess();
        assertTrue(Files.readString(directory.resolve("calls")).contains("--latest=false"));
        assertEquals(git(checkout, "rev-parse", "HEAD"), git(directory.resolve("remote.git"), "rev-parse", "v1"));
    }

    private TestProcesses.Result plan(String tag, String stableTags) throws Exception {
        Path tags = Files.writeString(directory.resolve("tags"), stableTags);
        Path output = directory.resolve("plan");
        Files.deleteIfExists(output);
        TestProcesses.Result result = TestProcesses.run(directory, Map.of(), List.of(java(), "-cp", compiled.toString(),
                "ReleaseVersion", tag, tags.toString(), output.toString()));
        return result.exitCode() == 0 ? new TestProcesses.Result(0, Files.readString(output)) : result;
    }

    private Path fixture(String stableTags) throws Exception {
        Path remote = directory.resolve("remote.git");
        Path checkout = directory.resolve("checkout");
        git(directory, "init", "--quiet", "--bare", remote.toString());
        git(directory, "init", "--quiet", "--initial-branch=main", checkout.toString());
        git(checkout, "remote", "add", "origin", remote.toString());
        git(checkout, "config", "user.name", "Fixture");
        git(checkout, "config", "user.email", "fixture@example.invalid");
        Path scripts = Files.createDirectories(checkout.resolve(".github/release"));
        for (String file : List.of("ReleaseVersion.java", "release.sh")) {
            Files.copy(project().resolve(".github/release/" + file), scripts.resolve(file));
        }
        git(checkout, "add", ".github");
        git(checkout, "commit", "--quiet", "-m", "Fixture");
        git(checkout, "push", "--quiet", "origin", "main");
        Files.writeString(directory.resolve("stable-tags"), stableTags);
        Path bin = Files.createDirectories(directory.resolve("bin"));
        Path gh = Files.writeString(bin.resolve("gh"), """
                #!/usr/bin/env bash
                set -euo pipefail
                case "$1 $2" in
                  'api --paginate') cat "$FIXTURE/stable-tags" ;;
                  'release view') test -f "$FIXTURE/published" && cat "$FIXTURE/published" ;;
                  'release create')
                    printf '%s\\n' "$@" >> "$FIXTURE/calls"
                    prerelease=false
                    for flag in "$@"; do
                      if [[ "$flag" == --prerelease ]]; then prerelease=true; fi
                    done
                    printf 'false\\t%s\\n' "$prerelease" > "$FIXTURE/published"
                    ;;
                  *) exit 2 ;;
                esac
                """);
        assertTrue(gh.toFile().setExecutable(true));
        return checkout;
    }

    private TestProcesses.Result publish(Path checkout, String tag) throws Exception {
        Map<String, String> env = new HashMap<>(gitEnvironment());
        env.put("PATH", directory.resolve("bin") + ":" + Path.of(java()).getParent() + ":" + System.getenv("PATH"));
        env.put("FIXTURE", directory.toString());
        env.put("TAG", tag);
        env.put("GH_REPO", "fixture/action");
        env.put("GITHUB_SHA", git(checkout, "rev-parse", "HEAD"));
        env.put("GITHUB_REF", "refs/heads/main");
        return TestProcesses.run(checkout, env, List.of("bash", ".github/release/release.sh", "publish"));
    }

    private String git(Path cwd, String... args) throws Exception {
        List<String> command = new ArrayList<>(List.of("git", "-C", cwd.toString()));
        command.addAll(List.of(args));
        return TestProcesses.run(cwd, gitEnvironment(), command).requireSuccess();
    }

    private Map<String, String> gitEnvironment() throws Exception {
        Path config = Files.writeString(directory.resolve("git-config"), "");
        return Map.of("GIT_CONFIG_GLOBAL", config.toString(), "GIT_CONFIG_NOSYSTEM", "1", "GIT_TERMINAL_PROMPT", "0");
    }

    private static Path project() {
        return Path.of(System.getProperty("city.test.project-directory"));
    }

    private static String java() {
        return Path.of(System.getProperty("java.home"), "bin", "java").toString();
    }
}
