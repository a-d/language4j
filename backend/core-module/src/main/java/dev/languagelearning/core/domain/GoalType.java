package dev.languagelearning.core.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

/**
 * Types of learning goals based on time period.
 */
@Getter
@RequiredArgsConstructor
public enum GoalType {

    /**
     * Daily learning goal.
     */
    DAILY("Daily", Duration.ofDays(1), ChronoUnit.DAYS),

    /**
     * Weekly learning goal.
     */
    WEEKLY("Weekly", Duration.ofDays(7), ChronoUnit.WEEKS),

    /**
     * Monthly learning goal.
     */
    MONTHLY("Monthly", Duration.ofDays(30), ChronoUnit.MONTHS),

    /**
     * Yearly learning goal.
     */
    YEARLY("Yearly", Duration.ofDays(365), ChronoUnit.YEARS);

    private final String displayName;
    private final Duration approximateDuration;
    private final ChronoUnit chronoUnit;

    /**
     * Checks if this goal type is shorter term than the given type.
     *
     * @param other the type to compare against
     * @return true if this type is shorter term
     */
    public boolean isShorterTermThan(GoalType other) {
        return this.approximateDuration.compareTo(other.approximateDuration) < 0;
    }

    /**
     * Checks if this goal type is longer term than the given type.
     *
     * @param other the type to compare against
     * @return true if this type is longer term
     */
    public boolean isLongerTermThan(GoalType other) {
        return this.approximateDuration.compareTo(other.approximateDuration) > 0;
    }
}