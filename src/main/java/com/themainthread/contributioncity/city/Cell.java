package com.themainthread.contributioncity.city;

import java.util.Objects;

public record Cell(char glyph, CellKind kind, int intensity, int buildingShade) {
    public static final Cell SKY = new Cell(' ', CellKind.SKY, 0, -1);

    public Cell {
        Objects.requireNonNull(kind, "kind");
        if (intensity < 0 || intensity > 4 || buildingShade < -1 || buildingShade > 1) {
            throw new IllegalArgumentException("Invalid cell intensity or building shade");
        }
    }
}
