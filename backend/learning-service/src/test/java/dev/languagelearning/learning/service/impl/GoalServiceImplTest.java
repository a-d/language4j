package dev.languagelearning.learning.service.impl;

import dev.languagelearning.core.domain.GoalType;
import dev.languagelearning.core.domain.LearningGoal;
import dev.languagelearning.core.domain.SkillLevel;
import dev.languagelearning.core.domain.User;
import dev.languagelearning.core.exception.EntityNotFoundException;
import dev.languagelearning.core.repository.LearningGoalRepository;
import dev.languagelearning.learning.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link GoalServiceImpl}.
 * <p>
 * Note: These tests are written based on the service implementation.
 * The service expects repository methods like findByUserId which may need
 * to be added to the actual repository interface.
 */
@ExtendWith(MockitoExtension.class)
class GoalServiceImplTest {

    @Mock
    private LearningGoalRepository goalRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private GoalServiceImpl goalService;

    @Captor
    private ArgumentCaptor<LearningGoal> goalCaptor;

    private User testUser;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testUser = createTestUser(testUserId);
        lenient().when(userService.getCurrentUser()).thenReturn(testUser);
    }

    @Nested
    @DisplayName("getCurrentUserGoals")
    class GetCurrentUserGoals {

        @Test
        @DisplayName("should return all goals for current user")
        void shouldReturnAllGoalsForCurrentUser() {
            // Given
            List<LearningGoal> expectedGoals = List.of(
                    createGoal(UUID.randomUUID(), "Goal 1", GoalType.DAILY),
                    createGoal(UUID.randomUUID(), "Goal 2", GoalType.WEEKLY)
            );
            when(goalRepository.findByUser(testUser)).thenReturn(expectedGoals);

            // When
            List<LearningGoal> result = goalService.getCurrentUserGoals();

            // Then
            assertThat(result).hasSize(2);
            assertThat(result).containsExactlyElementsOf(expectedGoals);
            verify(userService).getCurrentUser();
        }

        @Test
        @DisplayName("should return empty list when no goals exist")
        void shouldReturnEmptyListWhenNoGoalsExist() {
            // Given
            when(goalRepository.findByUser(testUser)).thenReturn(List.of());

            // When
            List<LearningGoal> result = goalService.getCurrentUserGoals();

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getCurrentUserGoalsByType")
    class GetCurrentUserGoalsByType {

        @Test
        @DisplayName("should return goals of specified type")
        void shouldReturnGoalsOfSpecifiedType() {
            // Given
            LearningGoal dailyGoal = createGoal(UUID.randomUUID(), "Daily Goal", GoalType.DAILY);
            when(goalRepository.findByUserAndGoalType(testUser, GoalType.DAILY))
                    .thenReturn(List.of(dailyGoal));

            // When
            List<LearningGoal> result = goalService.getCurrentUserGoalsByType(GoalType.DAILY);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getGoalType()).isEqualTo(GoalType.DAILY);
        }
    }

    @Nested
    @DisplayName("getActiveDailyGoals")
    class GetActiveDailyGoals {

        @Test
        @DisplayName("should return active daily goals for today")
        void shouldReturnActiveDailyGoalsForToday() {
            // Given
            LocalDate today = LocalDate.now();
            LearningGoal activeGoal = createGoal(UUID.randomUUID(), "Active Goal", GoalType.DAILY);
            when(goalRepository.findActiveGoals(eq(testUser), eq(today)))
                    .thenReturn(List.of(activeGoal));

            // When
            List<LearningGoal> result = goalService.getActiveDailyGoals();

            // Then
            assertThat(result).hasSize(1);
            verify(goalRepository).findActiveGoals(testUser, today);
        }
    }

    @Nested
    @DisplayName("createGoal")
    class CreateGoal {

        @Test
        @DisplayName("should create daily goal with correct date range")
        void shouldCreateDailyGoalWithCorrectDateRange() {
            // Given
            LocalDate today = LocalDate.now();
            when(goalRepository.save(any(LearningGoal.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            LearningGoal result = goalService.createGoal(
                    "Complete lessons",
                    "Learn something new",
                    GoalType.DAILY,
                    5,
                    "lessons"
            );

            // Then
            verify(goalRepository).save(goalCaptor.capture());
            LearningGoal savedGoal = goalCaptor.getValue();
            assertThat(savedGoal.getTitle()).isEqualTo("Complete lessons");
            assertThat(savedGoal.getDescription()).isEqualTo("Learn something new");
            assertThat(savedGoal.getGoalType()).isEqualTo(GoalType.DAILY);
            assertThat(savedGoal.getTargetValue()).isEqualTo(5);
            assertThat(savedGoal.getCurrentValue()).isZero();
            assertThat(savedGoal.getUnit()).isEqualTo("lessons");
            assertThat(savedGoal.getStartDate()).isEqualTo(today);
            assertThat(savedGoal.getEndDate()).isEqualTo(today);
            assertThat(savedGoal.isCompleted()).isFalse();
        }

        @Test
        @DisplayName("should create weekly goal ending on Sunday")
        void shouldCreateWeeklyGoalEndingOnSunday() {
            // Given
            LocalDate today = LocalDate.now();
            LocalDate expectedEndDate = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
            when(goalRepository.save(any(LearningGoal.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            goalService.createGoal("Weekly goal", null, GoalType.WEEKLY, 10, "exercises");

            // Then
            verify(goalRepository).save(goalCaptor.capture());
            LearningGoal savedGoal = goalCaptor.getValue();
            assertThat(savedGoal.getEndDate()).isEqualTo(expectedEndDate);
        }

        @Test
        @DisplayName("should create monthly goal ending at end of month")
        void shouldCreateMonthlyGoalEndingAtEndOfMonth() {
            // Given
            LocalDate today = LocalDate.now();
            LocalDate expectedEndDate = today.with(TemporalAdjusters.lastDayOfMonth());
            when(goalRepository.save(any(LearningGoal.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            goalService.createGoal("Monthly goal", null, GoalType.MONTHLY, 100, "words");

            // Then
            verify(goalRepository).save(goalCaptor.capture());
            LearningGoal savedGoal = goalCaptor.getValue();
            assertThat(savedGoal.getEndDate()).isEqualTo(expectedEndDate);
        }

        @Test
        @DisplayName("should create yearly goal ending at end of year")
        void shouldCreateYearlyGoalEndingAtEndOfYear() {
            // Given
            LocalDate today = LocalDate.now();
            LocalDate expectedEndDate = today.with(TemporalAdjusters.lastDayOfYear());
            when(goalRepository.save(any(LearningGoal.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            goalService.createGoal("Yearly goal", null, GoalType.YEARLY, 1000, "words");

            // Then
            verify(goalRepository).save(goalCaptor.capture());
            LearningGoal savedGoal = goalCaptor.getValue();
            assertThat(savedGoal.getEndDate()).isEqualTo(expectedEndDate);
        }

        @Test
        @DisplayName("should allow null description")
        void shouldAllowNullDescription() {
            // Given
            when(goalRepository.save(any(LearningGoal.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            goalService.createGoal("Goal without description", null, GoalType.DAILY, 5, "lessons");

            // Then
            verify(goalRepository).save(goalCaptor.capture());
            assertThat(goalCaptor.getValue().getDescription()).isNull();
        }
    }

    @Nested
    @DisplayName("updateProgress")
    class UpdateProgress {

        @Test
        @DisplayName("should update progress value")
        void shouldUpdateProgressValue() {
            // Given
            UUID goalId = UUID.randomUUID();
            LearningGoal goal = createGoal(goalId, "Test Goal", GoalType.DAILY);
            goal.setTargetValue(10);
            goal.setCurrentValue(3);
            when(goalRepository.findById(goalId)).thenReturn(Optional.of(goal));
            when(goalRepository.save(any(LearningGoal.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            LearningGoal result = goalService.updateProgress(goalId, 5);

            // Then
            assertThat(result.getCurrentValue()).isEqualTo(5);
            assertThat(result.isCompleted()).isFalse();
        }

        @Test
        @DisplayName("should auto-complete when target reached")
        void shouldAutoCompleteWhenTargetReached() {
            // Given
            UUID goalId = UUID.randomUUID();
            LearningGoal goal = createGoal(goalId, "Test Goal", GoalType.DAILY);
            goal.setTargetValue(10);
            goal.setCurrentValue(5);
            when(goalRepository.findById(goalId)).thenReturn(Optional.of(goal));
            when(goalRepository.save(any(LearningGoal.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            LearningGoal result = goalService.updateProgress(goalId, 10);

            // Then
            assertThat(result.isCompleted()).isTrue();
            assertThat(result.getCompletedAt()).isPresent();
        }

        @Test
        @DisplayName("should auto-complete when exceeding target")
        void shouldAutoCompleteWhenExceedingTarget() {
            // Given
            UUID goalId = UUID.randomUUID();
            LearningGoal goal = createGoal(goalId, "Test Goal", GoalType.DAILY);
            goal.setTargetValue(10);
            when(goalRepository.findById(goalId)).thenReturn(Optional.of(goal));
            when(goalRepository.save(any(LearningGoal.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            LearningGoal result = goalService.updateProgress(goalId, 15);

            // Then
            assertThat(result.isCompleted()).isTrue();
        }

        @Test
        @DisplayName("should not change completion status if already completed")
        void shouldNotChangeCompletionStatusIfAlreadyCompleted() {
            // Given
            UUID goalId = UUID.randomUUID();
            LearningGoal goal = createGoal(goalId, "Test Goal", GoalType.DAILY);
            goal.setTargetValue(10);
            goal.setCurrentValue(10);
            goal.setCompleted(true);
            Instant originalCompletedAt = Instant.now().minusSeconds(3600);
            goal.setCompletedAt(originalCompletedAt);
            when(goalRepository.findById(goalId)).thenReturn(Optional.of(goal));
            when(goalRepository.save(any(LearningGoal.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            LearningGoal result = goalService.updateProgress(goalId, 12);

            // Then
            assertThat(result.isCompleted()).isTrue();
            assertThat(result.getCompletedAt()).contains(originalCompletedAt);
        }

        @Test
        @DisplayName("should throw exception when goal not found")
        void shouldThrowExceptionWhenGoalNotFound() {
            // Given
            UUID goalId = UUID.randomUUID();
            when(goalRepository.findById(goalId)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> goalService.updateProgress(goalId, 5))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("incrementProgress")
    class IncrementProgress {

        @Test
        @DisplayName("should increment progress by specified amount")
        void shouldIncrementProgressBySpecifiedAmount() {
            // Given
            UUID goalId = UUID.randomUUID();
            LearningGoal goal = createGoal(goalId, "Test Goal", GoalType.DAILY);
            goal.setTargetValue(10);
            goal.setCurrentValue(3);
            when(goalRepository.findById(goalId)).thenReturn(Optional.of(goal));
            when(goalRepository.save(any(LearningGoal.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            LearningGoal result = goalService.incrementProgress(goalId, 2);

            // Then
            assertThat(result.getCurrentValue()).isEqualTo(5);
        }

        @Test
        @DisplayName("should auto-complete when increment reaches target")
        void shouldAutoCompleteWhenIncrementReachesTarget() {
            // Given
            UUID goalId = UUID.randomUUID();
            LearningGoal goal = createGoal(goalId, "Test Goal", GoalType.DAILY);
            goal.setTargetValue(10);
            goal.setCurrentValue(8);
            when(goalRepository.findById(goalId)).thenReturn(Optional.of(goal));
            when(goalRepository.save(any(LearningGoal.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            LearningGoal result = goalService.incrementProgress(goalId, 2);

            // Then
            assertThat(result.getCurrentValue()).isEqualTo(10);
            assertThat(result.isCompleted()).isTrue();
        }

        @Test
        @DisplayName("should throw exception when goal not found")
        void shouldThrowExceptionWhenGoalNotFound() {
            // Given
            UUID goalId = UUID.randomUUID();
            when(goalRepository.findById(goalId)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> goalService.incrementProgress(goalId, 1))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("completeGoal")
    class CompleteGoal {

        @Test
        @DisplayName("should mark goal as completed")
        void shouldMarkGoalAsCompleted() {
            // Given
            UUID goalId = UUID.randomUUID();
            LearningGoal goal = createGoal(goalId, "Test Goal", GoalType.DAILY);
            goal.setTargetValue(10);
            goal.setCurrentValue(5);
            when(goalRepository.findById(goalId)).thenReturn(Optional.of(goal));
            when(goalRepository.save(any(LearningGoal.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            LearningGoal result = goalService.completeGoal(goalId);

            // Then
            assertThat(result.isCompleted()).isTrue();
            assertThat(result.getCompletedAt()).isPresent();
            assertThat(result.getCurrentValue()).isEqualTo(result.getTargetValue());
        }

        @Test
        @DisplayName("should throw exception when goal not found")
        void shouldThrowExceptionWhenGoalNotFound() {
            // Given
            UUID goalId = UUID.randomUUID();
            when(goalRepository.findById(goalId)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> goalService.completeGoal(goalId))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("deleteGoal")
    class DeleteGoal {

        @Test
        @DisplayName("should delete goal by id")
        void shouldDeleteGoalById() {
            // Given
            UUID goalId = UUID.randomUUID();

            // When
            goalService.deleteGoal(goalId);

            // Then
            verify(goalRepository).deleteById(goalId);
        }
    }

    @Nested
    @DisplayName("createDefaultGoals")
    class CreateDefaultGoals {

        @Test
        @DisplayName("should create 5 default goals")
        void shouldCreate5DefaultGoals() {
            // Given
            when(userService.findById(testUserId)).thenReturn(Optional.of(testUser));
            when(goalRepository.save(any(LearningGoal.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            goalService.createDefaultGoals(testUserId);

            // Then
            verify(goalRepository, times(5)).save(any(LearningGoal.class));
        }

        @Test
        @DisplayName("should create 3 daily goals")
        void shouldCreate3DailyGoals() {
            // Given
            when(userService.findById(testUserId)).thenReturn(Optional.of(testUser));
            when(goalRepository.save(any(LearningGoal.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            goalService.createDefaultGoals(testUserId);

            // Then
            verify(goalRepository, times(5)).save(goalCaptor.capture());
            List<LearningGoal> savedGoals = goalCaptor.getAllValues();
            long dailyCount = savedGoals.stream()
                    .filter(g -> g.getGoalType() == GoalType.DAILY)
                    .count();
            assertThat(dailyCount).isEqualTo(3);
        }

        @Test
        @DisplayName("should create 1 weekly goal")
        void shouldCreate1WeeklyGoal() {
            // Given
            when(userService.findById(testUserId)).thenReturn(Optional.of(testUser));
            when(goalRepository.save(any(LearningGoal.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            goalService.createDefaultGoals(testUserId);

            // Then
            verify(goalRepository, times(5)).save(goalCaptor.capture());
            List<LearningGoal> savedGoals = goalCaptor.getAllValues();
            long weeklyCount = savedGoals.stream()
                    .filter(g -> g.getGoalType() == GoalType.WEEKLY)
                    .count();
            assertThat(weeklyCount).isEqualTo(1);
        }

        @Test
        @DisplayName("should create 1 monthly goal")
        void shouldCreate1MonthlyGoal() {
            // Given
            when(userService.findById(testUserId)).thenReturn(Optional.of(testUser));
            when(goalRepository.save(any(LearningGoal.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            goalService.createDefaultGoals(testUserId);

            // Then
            verify(goalRepository, times(5)).save(goalCaptor.capture());
            List<LearningGoal> savedGoals = goalCaptor.getAllValues();
            long monthlyCount = savedGoals.stream()
                    .filter(g -> g.getGoalType() == GoalType.MONTHLY)
                    .count();
            assertThat(monthlyCount).isEqualTo(1);
        }

        @Test
        @DisplayName("should throw exception when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            // Given
            UUID nonExistentUserId = UUID.randomUUID();
            when(userService.findById(nonExistentUserId)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> goalService.createDefaultGoals(nonExistentUserId))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    private User createTestUser(UUID id) {
        User user = new User();
        user.setId(id);
        user.setDisplayName("Test User");
        user.setNativeLanguage("de");
        user.setTargetLanguage("fr");
        user.setSkillLevel(SkillLevel.A1);
        return user;
    }

    private LearningGoal createGoal(UUID id, String title, GoalType type) {
        LearningGoal goal = new LearningGoal();
        goal.setId(id);
        goal.setUser(testUser);
        goal.setTitle(title);
        goal.setGoalType(type);
        goal.setTargetValue(5);
        goal.setCurrentValue(0);
        goal.setUnit("items");
        goal.setStartDate(LocalDate.now());
        goal.setEndDate(LocalDate.now());
        goal.setCompleted(false);
        return goal;
    }
}