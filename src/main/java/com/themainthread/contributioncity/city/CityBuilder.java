package com.themainthread.contributioncity.city;

import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import com.themainthread.contributioncity.model.ContributionCalendar;
import com.themainthread.contributioncity.model.ContributionDay;
import com.themainthread.contributioncity.model.ContributionWeek;

public class CityBuilder {
    private static final int BUILDING_WIDTH = 2;

    public Scene build(ContributionCalendar calendar, CityOptions options) {
        List<ContributionWeek> weeks = calendar.weeks().subList(
                Math.max(0, calendar.weeks().size() - options.weeks()), calendar.weeks().size());
        long[] totals = new long[weeks.size()];
        long maximum = 0;
        int recordWeek = -1;
        for (int i = 0; i < weeks.size(); i++) {
            totals[i] = weeks.get(i).total();
            // >= makes the most recent tied record win. No record for an inactive calendar.
            if (totals[i] > 0 && totals[i] >= maximum) {
                maximum = totals[i];
                recordWeek = i;
            }
        }

        int width = weeks.size() * BUILDING_WIDTH;
        int streetRow = options.maxHeight() + 2;
        Cell[][] grid = new Cell[streetRow + 2][width];
        for (Cell[] row : grid) {
            Arrays.fill(row, Cell.SKY);
        }
        double denominator = Math.log1p(maximum);
        for (int i = 0; i < weeks.size(); i++) {
            int height = totals[i] == 0 ? 1
                    : 1 + (int) Math.round(Math.log1p(totals[i]) / denominator * (options.maxHeight() - 1));
            int x = i * BUILDING_WIDTH;
            int roofRow = streetRow - height - 1;
            for (int column = 0; column < BUILDING_WIDTH; column++) {
                grid[roofRow][x + column] = new Cell('▁', CellKind.ROOF, 0, i % 2);
            }
            List<ContributionDay> days = weeks.get(i).days();
            for (int floor = 0; floor < height; floor++) {
                for (int column = 0; column < BUILDING_WIDTH; column++) {
                    ContributionDay day = days.get((floor * BUILDING_WIDTH + column) % days.size());
                    grid[streetRow - 1 - floor][x + column] = new Cell(day.count() == 0 ? '·' : '▪',
                            CellKind.WINDOW, day.level().intensity(), i % 2);
                }
            }
            if (i == recordWeek) {
                grid[roofRow - 1][x] = new Cell('╻', CellKind.ANTENNA, 0, i % 2);
            }
        }
        Arrays.fill(grid[streetRow], new Cell('━', CellKind.STREET, 0, -1));
        placeMonths(weeks, grid[streetRow + 1]);
        List<List<Cell>> rows = new ArrayList<>(grid.length);
        for (Cell[] row : grid) {
            rows.add(List.of(row));
        }
        return new Scene(calendar.username(), calendar.totalContributions(), rows);
    }

    private void placeMonths(List<ContributionWeek> weeks, Cell[] row) {
        YearMonth previous = null;
        int nextFree = 0;
        for (int i = 0; i < weeks.size(); i++) {
            // The first returned day handles a partial first week crossing a month boundary.
            YearMonth month = YearMonth.from(weeks.get(i).days().getFirst().date());
            if (!month.equals(previous)) {
                String label = month.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
                int x = i * BUILDING_WIDTH;
                if (x >= nextFree && x + label.length() <= row.length) {
                    for (int j = 0; j < label.length(); j++) {
                        row[x + j] = new Cell(label.charAt(j), CellKind.LABEL, 0, -1);
                    }
                    nextFree = x + label.length() + 1;
                }
            }
            previous = month;
        }
    }
}
