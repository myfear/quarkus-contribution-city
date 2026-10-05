package com.themainthread.contributioncity.model;

public enum ContributionLevel {
    NONE(0),
    FIRST_QUARTILE(1),
    SECOND_QUARTILE(2),
    THIRD_QUARTILE(3),
    FOURTH_QUARTILE(4);

    private final int intensity;

    ContributionLevel(int intensity) {
        this.intensity = intensity;
    }

    public int intensity() {
        return intensity;
    }
}
