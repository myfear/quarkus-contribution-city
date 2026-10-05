package com.themainthread.contributioncity.city;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.themainthread.contributioncity.Fixtures;
import com.themainthread.contributioncity.model.ContributionCalendar;
import com.themainthread.contributioncity.model.ContributionDay;
import com.themainthread.contributioncity.model.ContributionLevel;
import com.themainthread.contributioncity.model.ContributionWeek;
import com.themainthread.contributioncity.render.Themes;

class CityBuilderTest {
    private final CityBuilder builder = new CityBuilder();
    private final CityOptions options = new CityOptions(53, 14, Themes.GITHUB_DARK);

    @Test
    void logScalingKeepsQuietWeeksVisible() {
        Scene scene = builder.build(Fixtures.calendar(0, 3, 20), options);
        assertEquals(6, scene.width());
        assertEquals(1, height(scene, 0));
        assertEquals(7, height(scene, 1));
        assertEquals(14, height(scene, 2));
        assertEquals(0, scene.rows().getFirst().get(4).intensity());
        assertEquals(CellKind.ANTENNA, scene.rows().getFirst().get(4).kind());
        assertEquals(6, scene.rows().get(16).stream().filter(cell -> cell.kind() == CellKind.STREET).count());
    }

    @Test
    void selectsBeforeScalingAndTiesFavorMostRecent() {
        Scene scene = builder.build(Fixtures.calendar(5000, 0, 3, 20, 20), new CityOptions(4, 14, Themes.MONO));
        assertEquals(8, scene.width());
        assertEquals(14, height(scene, 3));
        assertEquals(14, height(scene, 2));
        assertEquals(CellKind.ANTENNA, scene.rows().getFirst().get(6).kind());
        assertEquals(CellKind.SKY, scene.rows().getFirst().get(4).kind());
        assertEquals(5043, scene.totalContributions());
    }

    @Test
    void allInactiveWeeksHaveNoRecordAntenna() {
        Scene scene = builder.build(Fixtures.calendar(0, 0, 0, 0), options);
        for (int i = 0; i < 4; i++) {
            assertEquals(1, height(scene, i));
        }
        assertEquals(0, scene.rows().stream().flatMap(List::stream).filter(cell -> cell.kind() == CellKind.ANTENNA).count());
    }

    @Test
    void repeatsDaysFromTheStreetUpWithoutPaddingPartialWeeks() {
        LocalDate start = LocalDate.of(2026, 1, 4);
        List<ContributionDay> days = List.of(new ContributionDay(start, 0, ContributionLevel.NONE),
                new ContributionDay(start.plusDays(1), 1, ContributionLevel.FIRST_QUARTILE),
                new ContributionDay(start.plusDays(2), 4, ContributionLevel.FOURTH_QUARTILE));
        Scene scene = builder.build(new ContributionCalendar("fixture-user", 5,
                List.of(new ContributionWeek(start, days))), new CityOptions(4, 3, Themes.MONO));
        assertEquals('·', scene.rows().get(4).get(0).glyph());
        assertEquals(1, scene.rows().get(4).get(1).intensity());
        assertEquals(4, scene.rows().get(3).get(0).intensity());
        assertEquals('·', scene.rows().get(3).get(1).glyph());
        assertEquals(1, scene.rows().get(2).get(0).intensity());
        assertEquals(4, scene.rows().get(2).get(1).intensity());
    }

    @Test
    void labelsUseActualFirstDayAndNeverChangeBuildingPositions() {
        LocalDate start = LocalDate.of(2026, 1, 25);
        ContributionCalendar calendar = new ContributionCalendar("fixture-user", 3, List.of(
                new ContributionWeek(start,
                        List.of(new ContributionDay(LocalDate.of(2026, 2, 1).minusDays(1), 1,
                                ContributionLevel.FIRST_QUARTILE))),
                new ContributionWeek(start.plusWeeks(1),
                        List.of(new ContributionDay(start.plusWeeks(1), 1, ContributionLevel.FIRST_QUARTILE))),
                new ContributionWeek(start.plusWeeks(2),
                        List.of(new ContributionDay(start.plusWeeks(2), 1, ContributionLevel.FIRST_QUARTILE)))));
        Scene scene = builder.build(calendar, options);
        assertEquals(6, scene.width());
        assertEquals("Jan   ", glyphs(scene.rows().getLast()));
        // Feb begins at column 2 and would overlap Jan. It must be skipped, not shifted.
        assertEquals(CellKind.ROOF, scene.rows().get(1).get(2).kind());
        ContributionCalendar partial = new ContributionCalendar("fixture-user", 1, List.of(
                new ContributionWeek(LocalDate.of(2026, 8, 30), List.of(
                        new ContributionDay(LocalDate.of(2026, 9, 1), 1, ContributionLevel.FIRST_QUARTILE))),
                new ContributionWeek(LocalDate.of(2026, 9, 6), List.of(
                        new ContributionDay(LocalDate.of(2026, 9, 6), 0, ContributionLevel.NONE)))));
        assertEquals("Sep ", glyphs(builder.build(partial, options).rows().getLast()));
    }

    @Test
    void neverClipsAMonthAtTheRightEdge() {
        Scene scene = builder.build(Fixtures.calendar(1, 1, 1, 1, 1), options);
        assertEquals("Jan       ", glyphs(scene.rows().getLast()));
    }

    @ParameterizedTest
    @ValueSource(ints = { 3, 14, 30 })
    void geometryIsBoundedAndStableForEveryHeight(int height) {
        CityOptions configuration = new CityOptions(53, height, Themes.GITHUB_DARK);
        ContributionCalendar calendar = Fixtures.calendar(0, 1, 10000);
        Scene scene = builder.build(calendar, configuration);
        assertEquals(scene, builder.build(calendar, configuration));
        assertEquals(height + 4, scene.rows().size());
        assertTrue(height(scene, 1) >= 1 && height(scene, 1) <= height);
        assertThrows(UnsupportedOperationException.class, () -> scene.rows().clear());
        assertThrows(UnsupportedOperationException.class, () -> scene.rows().getFirst().clear());
    }

    private int height(Scene scene, int week) {
        return (int) scene.rows().stream().filter(row -> row.get(week * 2).kind() == CellKind.WINDOW).count();
    }

    private String glyphs(List<Cell> row) {
        StringBuilder result = new StringBuilder();
        for (Cell cell : row) {
            result.append(cell.glyph());
        }
        return result.toString();
    }
}
