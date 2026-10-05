package com.themainthread.contributioncity.model;

import java.time.LocalDate;
import java.util.Objects;

public record ContributionDay(LocalDate date, int count, ContributionLevel level) {
    public ContributionDay {
        Objects.requireNonNull(date, "date");
        Objects.requireNonNull(level, "level");
        if (count < 0) {
            throw new IllegalArgumentException("Contribution count must not be negative");
        }
    }
}
