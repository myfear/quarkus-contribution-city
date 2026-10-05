package com.themainthread.contributioncity.city;

import java.util.List;
import java.util.Objects;

public record Scene(String username, int totalContributions, List<List<Cell>> rows) {
    public Scene {
        Objects.requireNonNull(username, "username");
        rows = rows.stream().map(List::copyOf).toList();
        if (rows.isEmpty() || rows.getFirst().isEmpty()) {
            throw new IllegalArgumentException("A scene must have cells");
        }
        int width = rows.getFirst().size();
        for (List<Cell> row : rows) {
            if (row.size() != width) {
                throw new IllegalArgumentException("Scene rows must have the same width");
            }
        }
    }

    public int width() {
        return rows.getFirst().size();
    }
}
