package dev.languagelearning.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.languagelearning.api.dto.CreateGoalRequest;
import dev.languagelearning.api.dto.UpdateGoalProgressRequest;
import dev.languagelearning.core.domain.GoalType;
import dev.languagelearning.core.domain.LearningGoal;
import dev.languagelearning.core.domain.SkillLevel;
import dev.languagelearning.core.domain.User;
import dev.languagelearning.learning.service.GoalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for {@link GoalController}.
 */
@ExtendWith(MockitoExtension.class)
class GoalControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private GoalService goalService;

    @InjectMocks
    private GoalController goalController;

    private User testUser;
    private LearningGoal testGoal;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(goalController).build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        testUser = createTestUser();
        testGoal = createTestGoal(GoalType.DAILY);
    }

    @Nested
    @DisplayName("GET /api/v1/goals")
    class GetGoals {

        @Test
        @DisplayName("should return all goals")
        void shouldReturnAllGoals() throws Exception {
            LearningGoal dailyGoal = createTestGoal(GoalType.DAILY);
            LearningGoal weeklyGoal = createTestGoal(GoalType.WEEKLY);
            when(goalService.getCurrentUserGoals()).thenReturn(List.of(dailyGoal, weeklyGoal));

            mockMvc.perform(get("/api/v1/goals"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].type").value("DAILY"))
                    .andExpect(jsonPath("$[1].type").value("WEEKLY"));

            verify(goalService).getCurrentUserGoals();
            verify(goalService, never()).getCurrentUserGoalsByType(any());
        }

        @Test
        @DisplayName("should filter by type when provided")
        void shouldFilterByTypeWhenProvided() throws Exception {
            LearningGoal dailyGoal = createTestGoal(GoalType.DAILY);
            when(goalService.getCurrentUserGoalsByType(GoalType.DAILY)).thenReturn(List.of(dailyGoal));

            mockMvc.perform(get("/api/v1/goals").param("type", "DAILY"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].type").value("DAILY"));

            verify(goalService).getCurrentUserGoalsByType(GoalType.DAILY);
            verify(goalService, never()).getCurrentUserGoals();
        }

        @Test
        @DisplayName("should handle case-insensitive type parameter")
        void shouldHandleCaseInsensitiveTypeParameter() throws Exception {
            LearningGoal weeklyGoal = createTestGoal(GoalType.WEEKLY);
            when(goalService.getCurrentUserGoalsByType(GoalType.WEEKLY)).thenReturn(List.of(weeklyGoal));

            mockMvc.perform(get("/api/v1/goals").param("type", "weekly"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].type").value("WEEKLY"));

            verify(goalService).getCurrentUserGoalsByType(GoalType.WEEKLY);
        }

        @Test
        @DisplayName("should return empty list when no goals")
        void shouldReturnEmptyListWhenNoGoals() throws Exception {
            when(goalService.getCurrentUserGoals()).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/goals"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/goals/daily/active")
    class GetActiveDailyGoals {

        @Test
        @DisplayName("should return active daily goals")
        void shouldReturnActiveDailyGoals() throws Exception {
            LearningGoal activeGoal = createTestGoal(GoalType.DAILY);
            when(goalService.getActiveDailyGoals()).thenReturn(List.of(activeGoal));

            mockMvc.perform(get("/api/v1/goals/daily/active"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].completed").value(false));

            verify(goalService).getActiveDailyGoals();
        }
    }

    @Nested
    @DisplayName("POST /api/v1/goals")
    class CreateGoal {

        @Test
        @DisplayName("should create goal with all fields")
        void shouldCreateGoalWithAllFields() throws Exception {
            CreateGoalRequest request = new CreateGoalRequest(
                    "Complete lessons",
                    "Learn new words",
                    "DAILY",
                    5,
                    "lessons"
            );
            when(goalService.createGoal(
                    eq("Complete lessons"),
                    eq("Learn new words"),
                    eq(GoalType.DAILY),
                    eq(5),
                    eq("lessons")
            )).thenReturn(testGoal);

            mockMvc.perform(post("/api/v1/goals")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value(testGoal.getTitle()))
                    .andExpect(jsonPath("$.type").value("DAILY"));

            verify(goalService).createGoal("Complete lessons", "Learn new words", GoalType.DAILY, 5, "lessons");
        }

        @Test
        @DisplayName("should create goal without description")
        void shouldCreateGoalWithoutDescription() throws Exception {
            CreateGoalRequest request = new CreateGoalRequest(
                    "Study vocabulary",
                    null,
                    "WEEKLY",
                    20,
                    "words"
            );
            LearningGoal weeklyGoal = createTestGoal(GoalType.WEEKLY);
            when(goalService.createGoal(
                    eq("Study vocabulary"),
                    isNull(),
                    eq(GoalType.WEEKLY),
                    eq(20),
                    eq("words")
            )).thenReturn(weeklyGoal);

            mockMvc.perform(post("/api/v1/goals")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.type").value("WEEKLY"));
        }

        @Test
        @DisplayName("should handle lowercase type")
        void shouldHandleLowercaseType() throws Exception {
            CreateGoalRequest request = new CreateGoalRequest(
                    "Monthly goal",
                    null,
                    "monthly",
                    100,
                    "words"
            );
            LearningGoal monthlyGoal = createTestGoal(GoalType.MONTHLY);
            when(goalService.createGoal(anyString(), any(), eq(GoalType.MONTHLY), anyInt(), anyString()))
                    .thenReturn(monthlyGoal);

            mockMvc.perform(post("/api/v1/goals")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.type").value("MONTHLY"));
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/goals/{goalId}/progress")
    class UpdateProgress {

        @Test
        @DisplayName("should update progress")
        void shouldUpdateProgress() throws Exception {
            UUID goalId = testGoal.getId();
            UpdateGoalProgressRequest request = new UpdateGoalProgressRequest(7);
            
            LearningGoal updatedGoal = createTestGoal(GoalType.DAILY);
            updatedGoal.setCurrentValue(7);
            
            when(goalService.updateProgress(goalId, 7)).thenReturn(updatedGoal);

            mockMvc.perform(patch("/api/v1/goals/{goalId}/progress", goalId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.currentValue").value(7));

            verify(goalService).updateProgress(goalId, 7);
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/goals/{goalId}/increment")
    class IncrementProgress {

        @Test
        @DisplayName("should increment with default amount")
        void shouldIncrementWithDefaultAmount() throws Exception {
            UUID goalId = testGoal.getId();
            LearningGoal updatedGoal = createTestGoal(GoalType.DAILY);
            updatedGoal.setCurrentValue(1);
            
            when(goalService.incrementProgress(goalId, 1)).thenReturn(updatedGoal);

            mockMvc.perform(patch("/api/v1/goals/{goalId}/increment", goalId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.currentValue").value(1));

            verify(goalService).incrementProgress(goalId, 1);
        }

        @Test
        @DisplayName("should increment with custom amount")
        void shouldIncrementWithCustomAmount() throws Exception {
            UUID goalId = testGoal.getId();
            LearningGoal updatedGoal = createTestGoal(GoalType.DAILY);
            updatedGoal.setCurrentValue(5);
            
            when(goalService.incrementProgress(goalId, 5)).thenReturn(updatedGoal);

            mockMvc.perform(patch("/api/v1/goals/{goalId}/increment", goalId)
                            .param("amount", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.currentValue").value(5));

            verify(goalService).incrementProgress(goalId, 5);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/goals/{goalId}/complete")
    class CompleteGoal {

        @Test
        @DisplayName("should complete goal")
        void shouldCompleteGoal() throws Exception {
            UUID goalId = testGoal.getId();
            LearningGoal completedGoal = createTestGoal(GoalType.DAILY);
            completedGoal.setCompleted(true);
            completedGoal.setCurrentValue(completedGoal.getTargetValue());
            completedGoal.setCompletedAt(Instant.now());
            
            when(goalService.completeGoal(goalId)).thenReturn(completedGoal);

            mockMvc.perform(post("/api/v1/goals/{goalId}/complete", goalId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.completed").value(true))
                    .andExpect(jsonPath("$.completedAt").exists());

            verify(goalService).completeGoal(goalId);
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/goals/{goalId}")
    class DeleteGoal {

        @Test
        @DisplayName("should delete goal")
        void shouldDeleteGoal() throws Exception {
            UUID goalId = testGoal.getId();
            doNothing().when(goalService).deleteGoal(goalId);

            mockMvc.perform(delete("/api/v1/goals/{goalId}", goalId))
                    .andExpect(status().isNoContent());

            verify(goalService).deleteGoal(goalId);
        }
    }

    @Nested
    @DisplayName("GoalDto mapping")
    class GoalDtoMapping {

        @Test
        @DisplayName("should calculate progress percentage")
        void shouldCalculateProgressPercentage() throws Exception {
            LearningGoal goal = createTestGoal(GoalType.DAILY);
            goal.setTargetValue(10);
            goal.setCurrentValue(5);
            when(goalService.getCurrentUserGoals()).thenReturn(List.of(goal));

            mockMvc.perform(get("/api/v1/goals"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].progressPercent").value(50));
        }

        @Test
        @DisplayName("should cap progress at 100 percent")
        void shouldCapProgressAt100Percent() throws Exception {
            LearningGoal goal = createTestGoal(GoalType.DAILY);
            goal.setTargetValue(10);
            goal.setCurrentValue(15);
            when(goalService.getCurrentUserGoals()).thenReturn(List.of(goal));

            mockMvc.perform(get("/api/v1/goals"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].progressPercent").value(100));
        }

        @Test
        @DisplayName("should include all goal fields in response")
        void shouldIncludeAllGoalFieldsInResponse() throws Exception {
            when(goalService.getCurrentUserGoals()).thenReturn(List.of(testGoal));

            mockMvc.perform(get("/api/v1/goals"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(testGoal.getId().toString()))
                    .andExpect(jsonPath("$[0].type").value(testGoal.getGoalType().name()))
                    .andExpect(jsonPath("$[0].title").value(testGoal.getTitle()))
                    .andExpect(jsonPath("$[0].targetValue").value(testGoal.getTargetValue()))
                    .andExpect(jsonPath("$[0].currentValue").value(testGoal.getCurrentValue()))
                    .andExpect(jsonPath("$[0].unit").value(testGoal.getUnit()))
                    .andExpect(jsonPath("$[0].completed").value(testGoal.isCompleted()));
        }
    }

    private User createTestUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setDisplayName("Test User");
        user.setNativeLanguage("de");
        user.setTargetLanguage("fr");
        user.setSkillLevel(SkillLevel.A1);
        return user;
    }

    private LearningGoal createTestGoal(GoalType type) {
        LearningGoal goal = new LearningGoal();
        goal.setId(UUID.randomUUID());
        goal.setUser(testUser);
        goal.setGoalType(type);
        goal.setTitle("Test Goal");
        goal.setDescription("Test Description");
        goal.setTargetValue(10);
        goal.setCurrentValue(0);
        goal.setUnit("items");
        goal.setStartDate(LocalDate.now());
        goal.setEndDate(LocalDate.now().plusDays(1));
        goal.setCompleted(false);
        return goal;
    }
}