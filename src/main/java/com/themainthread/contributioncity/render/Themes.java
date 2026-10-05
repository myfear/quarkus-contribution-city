package com.themainthread.contributioncity.render;

import java.util.List;

public final class Themes {
    public static final Theme GITHUB_DARK = new Theme("github-dark", "#0d1117", "#161b22", "#1c2128",
            "#8b949e", "#6e7681", "#c9d1d9", List.of("#484f58", "#0e4429", "#006d32", "#26a641", "#39d353"));
    public static final Theme MONO = new Theme("mono", "#101010", "#202020", "#292929",
            "#aaaaaa", "#888888", "#eeeeee", List.of("#555555", "#777777", "#aaaaaa", "#cccccc", "#ffffff"));

    private Themes() {
    }

    public static Theme named(String name) {
        return switch (name) {
            case "github-dark" -> GITHUB_DARK;
            case "mono" -> MONO;
            default -> throw new IllegalArgumentException(
                    "Unknown theme '" + name + "'. Supported themes: github-dark, mono");
        };
    }
}
