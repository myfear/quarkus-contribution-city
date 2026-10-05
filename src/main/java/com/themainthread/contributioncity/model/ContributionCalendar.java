package com.themainthread.contributioncity.model;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public record ContributionCalendar(String username, int totalContributions, List<ContributionWeek> weeks) {
    public ContributionCalendar {
        Objects.requireNonNull(username, "username");
        weeks = List.copyOf(weeks);
        if (totalContributions < 0 || weeks.isEmpty()) {
            throw new IllegalArgumentException("Calendar must contain weeks and a nonnegative total");
        }
        LocalDate previous = null;
        for (ContributionWeek week : weeks) {
            if (previous != null && !week.firstDay().equals(previous.plusDays(7))) {
                throw new IllegalArgumentException("Contribution weeks must be consecutive and oldest first");
            }
            previous = week.firstDay();
        }
    }
}
