package com.themainthread.contributioncity.action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.quarkiverse.githubaction.testing.DefaultTestInputs;

class ActionInputsTest {
    @Test
    void trimsUsernameAndAppliesDefaults() {
        ActionInputs inputs = ActionInputs
                .read(new DefaultTestInputs(Map.of("username", "  myfear  ", "github-token", "test-token")));
        assertEquals("myfear", inputs.username());
        assertEquals(53, inputs.options().weeks());
        assertEquals(14, inputs.options().maxHeight());
        assertEquals("github-dark", inputs.options().theme().name());
    }

    @ParameterizedTest
    @CsvSource({ "username, ''", "username, 'bad/user'", "username, 'bad name'", "weeks, 3", "weeks, 54", "weeks, banana",
            "height, 2", "height, 31", "height, 3.5", "theme, matrix2", "github-token, ''" })
    void rejectsInvalidValues(String name, String value) {
        Map<String, String> inputs = new HashMap<>(Map.of("username", "myfear", "github-token", "test-token"));
        inputs.put(name, value);
        assertThrows(IllegalArgumentException.class, () -> ActionInputs.read(new DefaultTestInputs(inputs)));
    }

    @Test
    void rejectsTooLongAUsername() {
        assertThrows(IllegalArgumentException.class, () -> ActionInputs.read(
                new DefaultTestInputs(Map.of("username", "a".repeat(40), "github-token", "test-token"))));
    }
}
