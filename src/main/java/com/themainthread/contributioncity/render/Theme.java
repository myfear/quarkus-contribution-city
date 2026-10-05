package com.themainthread.contributioncity.render;

import java.util.ArrayList;
import java.util.List;

public record Theme(String name, String background, String buildingA, String buildingB,
        String roof, String street, String label, List<String> windows) {
    public Theme {
        windows = List.copyOf(windows);
        if (windows.size() != 5) {
            throw new IllegalArgumentException("A theme must have five window colors");
        }
        List<String> colors = new ArrayList<>(List.of(background, buildingA, buildingB, roof, street, label));
        colors.addAll(windows);
        for (String color : colors) {
            if (!color.matches("#[0-9a-fA-F]{6}")) {
                throw new IllegalArgumentException("Theme colors must be six-digit CSS hex colors");
            }
        }
    }
}
