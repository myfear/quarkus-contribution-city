package com.themainthread.contributioncity.github;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.themainthread.contributioncity.Fixtures;
import com.themainthread.contributioncity.model.ContributionCalendar;
import com.themainthread.contributioncity.model.ContributionLevel;

import jakarta.json.Json;
import jakarta.json.JsonObject;

class GitHubContributionParserTest {
    private final GitHubContributionParser parser = new GitHubContributionParser();

    @Test
    void parsesAllFiveLevelsAndImmutableWeeks() {
        ContributionCalendar calendar = parser.parse("fixture-user",
                Fixtures.response("contribution-calendar.json").getJsonObject("data"));
        assertEquals("fixture-user", calendar.username());
        assertEquals(44, calendar.totalContributions());
        assertEquals(4, calendar.weeks().size());
        assertEquals(LocalDate.of(2026, 1, 4), calendar.weeks().getFirst().firstDay());
        assertEquals(4, calendar.weeks().getFirst().days().get(4).count());
        assertEquals(Set.of(ContributionLevel.values()), calendar.weeks().getFirst().days().stream()
                .map(day -> day.level()).collect(Collectors.toSet()));
        assertThrows(UnsupportedOperationException.class, () -> calendar.weeks().clear());
        assertThrows(UnsupportedOperationException.class, () -> calendar.weeks().getFirst().days().clear());
    }

    @Test
    void parsesInactiveCalendar() {
        ContributionCalendar calendar = parser.parse("fixture-user",
                Fixtures.response("empty-calendar.json").getJsonObject("data"));
        assertEquals(0, calendar.totalContributions());
        assertTrue(calendar.weeks().stream().allMatch(week -> week.total() == 0));
    }

    @Test
    void rejectsUnknownUser() {
        assertTrue(assertThrows(IllegalArgumentException.class, () -> parser.parse("missing-user",
                Fixtures.response("graphql-error.json").getJsonObject("data"))).getMessage().contains("was not found"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "FIFTH_QUARTILE", "none", "" })
    void rejectsUnknownLevels(String level) {
        String response = Fixtures.json("contribution-calendar.json").replace("FOURTH_QUARTILE", level);
        assertTrue(assertThrows(IllegalArgumentException.class, () -> parser.parse("fixture-user", data(response)))
                .getMessage().contains("Unknown contribution level"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "{}", "{\"user\":{}}", "{\"user\":{\"contributionsCollection\":null}}" })
    void rejectsMissingFields(String json) {
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> parser.parse("fixture-user", Json.createReader(new StringReader(json)).readObject()))
                .getMessage().contains("Malformed GitHub contribution data"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "-1", "1.5", "2147483648", "\"12\"", "null" })
    void rejectsInvalidCounts(String count) {
        String response = Fixtures.json("contribution-calendar.json").replace("\"contributionCount\": 4",
                "\"contributionCount\": " + count);
        assertThrows(IllegalArgumentException.class, () -> parser.parse("fixture-user", data(response)));
    }

    @Test
    void rejectsMalformedDatesAndEmptyDays() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse("fixture-user",
                data(Fixtures.json("contribution-calendar.json").replace("2026-01-04", "not-a-date"))));
        JsonObject week = Json.createObjectBuilder().add("firstDay", "2026-01-04")
                .add("contributionDays", Json.createArrayBuilder()).build();
        JsonObject calendar = Json.createObjectBuilder().add("totalContributions", 0)
                .add("weeks", Json.createArrayBuilder().add(week)).build();
        JsonObject data = Json.createObjectBuilder().add("user", Json.createObjectBuilder()
                .add("contributionsCollection", Json.createObjectBuilder().add("contributionCalendar", calendar))).build();
        assertThrows(IllegalArgumentException.class, () -> parser.parse("fixture-user", data));
        assertThrows(IllegalArgumentException.class, () -> parser.parse("fixture-user", null));
    }

    private JsonObject data(String response) {
        return Json.createReader(new StringReader(response)).readObject().getJsonObject("data");
    }
}
