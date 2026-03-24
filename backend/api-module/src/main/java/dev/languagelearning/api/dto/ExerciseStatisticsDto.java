package dev.languagelearning.api.dto;

import dev.languagelearning.learning.service.ExerciseResultService.ExerciseStatistics;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

/**
 * DTO for exercise statistics.
 *
 * @param totalExercises   total number of exercises completed
 * @param exercisesToday   exercises completed today
 * @param averageScore     overall average score
 * @param totalTimeSeconds total time spent in seconds
 * @param passRate         percentage of exercises passed (last 30 days)
 * @param countsByType     counts per exercise type
 */
@Schema(description = "Exercise statistics summary")
public record ExerciseStatisticsDto(
        @Schema(description = "Total number of exercises completed", example = "150")
        long totalExercises,

        @Schema(description = "Exercises completed today", example = "5")
        long exercisesToday,

        @Schema(description = "Overall average score (0-100)", example = "78.5")
        Double averageScore,

        @Schema(description = "Total time spent in seconds", example = "7200")
        long totalTimeSeconds,

        @Schema(description = "Percentage of exercises passed (last 30 days)", example = "85.0")
        Double passRate,

        @Schema(description = "Exercise counts grouped by type")
        Map<String, Long> countsByType
) {
    /**
     * Creates a DTO from an ExerciseStatistics record.
     *
     * @param stats the statistics to convert
     * @return the DTO
     */
    public static ExerciseStatisticsDto from(ExerciseStatistics stats) {
        // Convert ExerciseType keys to String keys
        Map<String, Long> typeCountsAsStrings = stats.countsByType().entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        e -> e.getKey().name(),
                        Map.Entry::getValue
                ));

        return new ExerciseStatisticsDto(
                stats.totalExercises(),
                stats.exercisesToday(),
                stats.averageScore(),
                stats.totalTimeSeconds(),
                stats.passRate(),
                typeCountsAsStrings
        );
    }
}