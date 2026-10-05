package com.themainthread.contributioncity.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.themainthread.contributioncity.city.CityBuilder;
import com.themainthread.contributioncity.city.CityOptions;
import com.themainthread.contributioncity.model.ContributionCalendar;
import com.themainthread.contributioncity.model.ContributionDay;
import com.themainthread.contributioncity.model.ContributionLevel;
import com.themainthread.contributioncity.model.ContributionWeek;

class PreviewTest {
    @Test
    void writesReproducibleSyntheticLayoutSamples() throws Exception {
        Path directory = Path.of("target/previews");
        Files.createDirectories(directory);
        ContributionCalendar calendar = sample(false);
        write(directory, "github-dark.svg", calendar, 53, 14, Themes.GITHUB_DARK);
        write(directory, "mono.svg", calendar, 53, 14, Themes.MONO);
        write(directory, "recent-12.svg", calendar, 12, 14, Themes.GITHUB_DARK);
        write(directory, "short-4.svg", calendar, 4, 3, Themes.GITHUB_DARK);
        write(directory, "empty.svg", sample(true), 53, 14, Themes.GITHUB_DARK);
        write(directory, "maximum-height.svg", calendar, 53, 30, Themes.GITHUB_DARK);
    }

    private void write(Path directory, String name, ContributionCalendar calendar, int weeks, int height, Theme theme)
            throws Exception {
        CityBuilder builder = new CityBuilder();
        CityOptions options = new CityOptions(weeks, height, theme);
        SvgRenderer renderer = new SvgRenderer();
        String svg = renderer.render(builder.build(calendar, options), theme);
        assertEquals(svg, renderer.render(builder.build(calendar, options), theme));
        Files.writeString(directory.resolve(name), svg);
    }

    private ContributionCalendar sample(boolean inactive) {
        List<ContributionWeek> weeks = new ArrayList<>();
        int total = 0;
        for (int week = 0; week < 53; week++) {
            LocalDate start = LocalDate.of(2025, 10, 5).plusWeeks(week);
            List<ContributionDay> days = new ArrayList<>();
            int activity = week % 11 == 0 ? 0 : 1 + (week * 7) % 18;
            for (int day = 0; day < 7; day++) {
                int count = inactive || activity == 0 || (week + day) % 5 == 0 ? 0
                        : activity + (week * 3 + day * 5) % 9;
                // One deliberate outlier tests compression without pretending this is live account data.
                if (!inactive && week == 38) {
                    count += 90;
                }
                int level = count == 0 ? 0 : Math.min(4, 1 + count / 8);
                days.add(new ContributionDay(start.plusDays(day), count, ContributionLevel.values()[level]));
                total += count;
            }
            weeks.add(new ContributionWeek(start, days));
        }
        return new ContributionCalendar("sample-developer", total, weeks);
    }
}
