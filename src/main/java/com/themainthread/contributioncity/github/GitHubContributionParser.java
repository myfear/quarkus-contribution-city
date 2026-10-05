package com.themainthread.contributioncity.github;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.themainthread.contributioncity.model.ContributionCalendar;
import com.themainthread.contributioncity.model.ContributionDay;
import com.themainthread.contributioncity.model.ContributionLevel;
import com.themainthread.contributioncity.model.ContributionWeek;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.json.JsonArray;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;

@ApplicationScoped
public class GitHubContributionParser {
    public ContributionCalendar parse(String username, JsonObject data) {
        if (data == null) {
            throw new IllegalArgumentException("Missing GitHub contribution data");
        }
        if (data.containsKey("user") && data.isNull("user")) {
            throw new IllegalArgumentException("GitHub user '" + username + "' was not found");
        }
        try {
            JsonObject user = object(data.get("user"), "user");
            JsonObject collection = object(user.get("contributionsCollection"), "contributionsCollection");
            JsonObject calendar = object(collection.get("contributionCalendar"), "contributionCalendar");
            List<ContributionWeek> weeks = new ArrayList<>();
            for (JsonValue value : array(calendar.get("weeks"), "weeks")) {
                JsonObject week = object(value, "week");
                List<ContributionDay> days = new ArrayList<>();
                for (JsonValue dayValue : array(week.get("contributionDays"), "contributionDays")) {
                    JsonObject day = object(dayValue, "contributionDay");
                    String levelName = string(day.get("contributionLevel"), "contributionLevel");
                    ContributionLevel level;
                    try {
                        level = ContributionLevel.valueOf(levelName);
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException("Unknown contribution level '" + levelName + "'");
                    }
                    days.add(new ContributionDay(LocalDate.parse(string(day.get("date"), "date")),
                            number(day.get("contributionCount"), "contributionCount"), level));
                }
                weeks.add(new ContributionWeek(LocalDate.parse(string(week.get("firstDay"), "firstDay")), days));
            }
            return new ContributionCalendar(username, number(calendar.get("totalContributions"), "totalContributions"), weeks);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Malformed GitHub contribution data: " + e.getMessage());
        }
    }

    private JsonObject object(JsonValue value, String field) {
        if (value instanceof JsonObject object) {
            return object;
        }
        throw new IllegalArgumentException("Missing or invalid " + field);
    }

    private JsonArray array(JsonValue value, String field) {
        if (value instanceof JsonArray array) {
            return array;
        }
        throw new IllegalArgumentException("Missing or invalid " + field);
    }

    private String string(JsonValue value, String field) {
        if (value instanceof JsonString string) {
            return string.getString();
        }
        throw new IllegalArgumentException("Missing or invalid " + field);
    }

    private int number(JsonValue value, String field) {
        if (value instanceof JsonNumber number && number.isIntegral()) {
            return number.intValueExact();
        }
        throw new IllegalArgumentException("Missing or invalid " + field);
    }
}
