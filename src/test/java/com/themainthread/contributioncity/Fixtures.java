package com.themainthread.contributioncity;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.themainthread.contributioncity.model.ContributionCalendar;
import com.themainthread.contributioncity.model.ContributionDay;
import com.themainthread.contributioncity.model.ContributionLevel;
import com.themainthread.contributioncity.model.ContributionWeek;

import jakarta.json.Json;
import jakarta.json.JsonObject;

public final class Fixtures {
    private Fixtures() {
    }

    public static String json(String name) {
        try (InputStream stream = Fixtures.class.getResourceAsStream("/github/" + name)) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static JsonObject response(String name) {
        return Json.createReader(new java.io.StringReader(json(name))).readObject();
    }

    public static ContributionCalendar calendar(int... counts) {
        List<ContributionWeek> weeks = new ArrayList<>();
        int total = 0;
        for (int i = 0; i < counts.length; i++) {
            LocalDate start = LocalDate.of(2026, 1, 4).plusWeeks(i);
            weeks.add(new ContributionWeek(start, List.of(new ContributionDay(start, counts[i],
                    counts[i] == 0 ? ContributionLevel.NONE : ContributionLevel.FOURTH_QUARTILE))));
            total += counts[i];
        }
        return new ContributionCalendar("fixture-user", total, weeks);
    }
}
