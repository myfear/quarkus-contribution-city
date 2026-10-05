package com.themainthread.contributioncity.model;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public record ContributionWeek(LocalDate firstDay, List<ContributionDay> days) {
    public ContributionWeek {
        Objects.requireNonNull(firstDay, "firstDay");
        days = List.copyOf(days);
        if (days.isEmpty() || days.size() > 7) {
            throw new IllegalArgumentException("A contribution week must contain 1..7 days");
        }
        LocalDate previous = null;
        for (ContributionDay day : days) {
            if (day.date().isBefore(firstDay) || !day.date().isBefore(firstDay.plusDays(7))
                    || (previous != null && !day.date().equals(previous.plusDays(1)))) {
                throw new IllegalArgumentException("Contribution days must be consecutive and inside their week");
            }
            previous = day.date();
        }
    }

    public long total() {
        long total = 0;
        for (ContributionDay day : days) {
            total += day.count();
        }
        return total;
    }
}
