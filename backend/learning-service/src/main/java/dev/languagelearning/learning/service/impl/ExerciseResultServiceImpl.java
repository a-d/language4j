package dev.languagelearning.learning.service.impl;

import dev.languagelearning.core.domain.ExerciseResult;
import dev.languagelearning.core.domain.ExerciseType;
import dev.languagelearning.core.domain.User;
import dev.languagelearning.core.repository.ExerciseResultRepository;
import dev.languagelearning.learning.service.ExerciseResultService;
import dev.languagelearning.learning.service.UserService;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of {@link ExerciseResultService}.
 * <p>
 * Manages exercise results storage, retrieval, and statistics calculation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExerciseResultServiceImpl implements ExerciseResultService {

    private static final int PASSING_SCORE = 70;

    private final ExerciseResultRepository exerciseResultRepository;
    private final UserService userService;

    @Override
    @Nonnull
    @Transactional
    public ExerciseResult saveResult(
            @Nonnull ExerciseType exerciseType,
            int score,
            int correctAnswers,
            int totalQuestions,
            long timeSpentSeconds,
            String userResponse,
            String correctResponse,
            String feedback
    ) {
        return saveResult(
                exerciseType,
                null,
                score,
                correctAnswers,
                totalQuestions,
                timeSpentSeconds,
                userResponse,
                correctResponse,
                feedback
        );
    }

    @Override
    @Nonnull
    @Transactional
    public ExerciseResult saveResult(
            @Nonnull ExerciseType exerciseType,
            String exerciseReference,
            int score,
            int correctAnswers,
            int totalQuestions,
            long timeSpentSeconds,
            String userResponse,
            String correctResponse,
            String feedback
    ) {
        User currentUser = userService.getCurrentUser();

        ExerciseResult result = new ExerciseResult();
        result.setUser(currentUser);
        result.setExerciseType(exerciseType);
        result.setExerciseReference(exerciseReference);
        result.setScore(Math.min(100, Math.max(0, score)));
        result.setMaxScore(100);
        result.setCorrectAnswers(correctAnswers);
        result.setTotalQuestions(totalQuestions);
        result.setTimeSpentSeconds(timeSpentSeconds);
        result.setUserResponse(userResponse);
        result.setCorrectResponse(correctResponse);
        result.setFeedback(feedback);
        result.setSkillLevelAtTime(currentUser.getSkillLevel());
        result.setPassed(score >= PASSING_SCORE);

        ExerciseResult saved = exerciseResultRepository.save(result);

        log.info("Saved exercise result: type={}, score={}, passed={}, userId={}",
                exerciseType, score, saved.isPassed(), currentUser.getId());

        return saved;
    }

    @Override
    @Nonnull
    @Transactional(readOnly = true)
    public Optional<ExerciseResult> findById(@Nonnull UUID id) {
        return exerciseResultRepository.findById(id);
    }

    @Override
    @Nonnull
    @Transactional(readOnly = true)
    public Page<ExerciseResult> getHistory(@Nonnull Pageable pageable) {
        User currentUser = userService.getCurrentUser();
        return exerciseResultRepository.findByUserOrderByCreatedAtDesc(currentUser, pageable);
    }

    @Override
    @Nonnull
    @Transactional(readOnly = true)
    public Page<ExerciseResult> getHistoryByType(@Nonnull ExerciseType exerciseType, @Nonnull Pageable pageable) {
        User currentUser = userService.getCurrentUser();
        return exerciseResultRepository.findByUserAndExerciseTypeOrderByCreatedAtDesc(
                currentUser, exerciseType, pageable
        );
    }

    @Override
    @Nonnull
    @Transactional(readOnly = true)
    public List<ExerciseResult> getRecentResults(@Nonnull Instant since) {
        User currentUser = userService.getCurrentUser();
        return exerciseResultRepository.findByUserAndCreatedAtAfterOrderByCreatedAtDesc(currentUser, since);
    }

    @Override
    @Nonnull
    @Transactional(readOnly = true)
    public List<ExerciseResult> getRecentResults(int days) {
        Instant since = Instant.now().minus(Duration.ofDays(days));
        return getRecentResults(since);
    }

    @Override
    @Nonnull
    @Transactional(readOnly = true)
    public Optional<Double> getAverageScore(@Nonnull ExerciseType exerciseType) {
        User currentUser = userService.getCurrentUser();
        Double avg = exerciseResultRepository.calculateAverageScore(currentUser, exerciseType);
        return Optional.ofNullable(avg);
    }

    @Override
    @Nonnull
    @Transactional(readOnly = true)
    public Optional<Double> getOverallAverageScore() {
        User currentUser = userService.getCurrentUser();
        Double avg = exerciseResultRepository.calculateOverallAverageScore(currentUser);
        return Optional.ofNullable(avg);
    }

    @Override
    @Transactional(readOnly = true)
    public long getTotalTimeSpentSeconds() {
        User currentUser = userService.getCurrentUser();
        return exerciseResultRepository.getTotalTimeSpentSeconds(currentUser);
    }

    @Override
    @Transactional(readOnly = true)
    public long countExercisesInRange(@Nonnull Instant start, @Nonnull Instant end) {
        User currentUser = userService.getCurrentUser();
        return exerciseResultRepository.countExercisesInRange(currentUser, start, end);
    }

    @Override
    @Transactional(readOnly = true)
    public long countExercisesToday() {
        LocalDate today = LocalDate.now();
        Instant startOfDay = today.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant endOfDay = today.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        return countExercisesInRange(startOfDay, endOfDay);
    }

    @Override
    @Nonnull
    @Transactional(readOnly = true)
    public Map<ExerciseType, Long> getCountsByType() {
        User currentUser = userService.getCurrentUser();
        List<Object[]> results = exerciseResultRepository.countByExerciseType(currentUser);

        Map<ExerciseType, Long> counts = new HashMap<>();
        for (Object[] row : results) {
            ExerciseType type = (ExerciseType) row[0];
            Long count = (Long) row[1];
            counts.put(type, count);
        }
        return counts;
    }

    @Override
    @Nonnull
    @Transactional(readOnly = true)
    public ExerciseStatistics getStatistics() {
        User currentUser = userService.getCurrentUser();

        // Get total count
        long totalExercises = exerciseResultRepository.count();

        // Get today's count
        long exercisesToday = countExercisesToday();

        // Get average score
        Double averageScore = exerciseResultRepository.calculateOverallAverageScore(currentUser);

        // Get total time
        long totalTimeSeconds = exerciseResultRepository.getTotalTimeSpentSeconds(currentUser);

        // Calculate pass rate
        Double passRate = null;
        if (totalExercises > 0) {
            LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);
            Instant start = thirtyDaysAgo.atStartOfDay().toInstant(ZoneOffset.UTC);
            Instant end = Instant.now();

            long totalInRange = exerciseResultRepository.countExercisesInRange(currentUser, start, end);
            if (totalInRange > 0) {
                long passedInRange = exerciseResultRepository.countPassedExercisesInRange(currentUser, start, end);
                passRate = (passedInRange * 100.0) / totalInRange;
            }
        }

        // Get counts by type
        Map<ExerciseType, Long> countsByType = getCountsByType();

        return new ExerciseStatistics(
                totalExercises,
                exercisesToday,
                averageScore,
                totalTimeSeconds,
                passRate,
                countsByType
        );
    }
}