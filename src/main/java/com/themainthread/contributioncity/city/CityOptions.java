package com.themainthread.contributioncity.city;

import java.util.Objects;

import com.themainthread.contributioncity.render.Theme;

public record CityOptions(int weeks, int maxHeight, Theme theme) {
    public CityOptions {
        if (weeks < 4 || weeks > 53) {
            throw new IllegalArgumentException("weeks must be between 4 and 53");
        }
        if (maxHeight < 3 || maxHeight > 30) {
            throw new IllegalArgumentException("height must be between 3 and 30");
        }
        Objects.requireNonNull(theme, "theme");
    }
}
