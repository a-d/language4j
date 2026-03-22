package dev.languagelearning.core.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Language skill levels based on CEFR (Common European Framework of Reference).
 * <p>
 * Levels range from A1 (beginner) to C2 (mastery).
 */
@Getter
@RequiredArgsConstructor
public enum SkillLevel {

    /**
     * Beginner - Can understand and use familiar everyday expressions.
     */
    A1("Beginner", 1, "Can understand and use familiar everyday expressions and very basic phrases."),

    /**
     * Elementary - Can understand sentences related to areas of immediate relevance.
     */
    A2("Elementary", 2, "Can understand sentences and frequently used expressions related to areas of most immediate relevance."),

    /**
     * Intermediate - Can deal with most situations likely to arise while traveling.
     */
    B1("Intermediate", 3, "Can deal with most situations likely to arise whilst travelling in an area where the language is spoken."),

    /**
     * Upper Intermediate - Can interact with a degree of fluency and spontaneity.
     */
    B2("Upper Intermediate", 4, "Can interact with a degree of fluency and spontaneity that makes regular interaction with native speakers possible."),

    /**
     * Advanced - Can express ideas fluently and spontaneously.
     */
    C1("Advanced", 5, "Can express ideas fluently and spontaneously without much obvious searching for expressions."),

    /**
     * Mastery - Can understand with ease virtually everything heard or read.
     */
    C2("Mastery", 6, "Can understand with ease virtually everything heard or read and express themselves spontaneously.");

    private final String displayName;
    private final int numericLevel;
    private final String description;

    /**
     * Gets the next higher skill level.
     *
     * @return the next skill level, or the same level if already at maximum
     */
    public SkillLevel next() {
        return switch (this) {
            case A1 -> A2;
            case A2 -> B1;
            case B1 -> B2;
            case B2 -> C1;
            case C1, C2 -> C2;
        };
    }

    /**
     * Gets the previous lower skill level.
     *
     * @return the previous skill level, or the same level if already at minimum
     */
    public SkillLevel previous() {
        return switch (this) {
            case A1, A2 -> A1;
            case B1 -> A2;
            case B2 -> B1;
            case C1 -> B2;
            case C2 -> C1;
        };
    }

    /**
     * Checks if this level is higher than the given level.
     *
     * @param other the level to compare against
     * @return true if this level is higher
     */
    public boolean isHigherThan(SkillLevel other) {
        return this.numericLevel > other.numericLevel;
    }

    /**
     * Checks if this level is lower than the given level.
     *
     * @param other the level to compare against
     * @return true if this level is lower
     */
    public boolean isLowerThan(SkillLevel other) {
        return this.numericLevel < other.numericLevel;
    }

    /**
     * Gets skill level from numeric value.
     *
     * @param level numeric level (1-6)
     * @return corresponding skill level
     * @throws IllegalArgumentException if level is out of range
     */
    public static SkillLevel fromNumericLevel(int level) {
        return switch (level) {
            case 1 -> A1;
            case 2 -> A2;
            case 3 -> B1;
            case 4 -> B2;
            case 5 -> C1;
            case 6 -> C2;
            default -> throw new IllegalArgumentException("Invalid skill level: " + level + ". Must be between 1 and 6.");
        };
    }
}