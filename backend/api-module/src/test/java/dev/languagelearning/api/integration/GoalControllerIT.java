package dev.languagelearning.api.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.languagelearning.api.dto.CreateGoalRequest;
import dev.languagelearning.api.dto.UpdateGoalProgressRequest;
import dev.languagelearning.core.domain.GoalType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@link dev.languagelearning.api.controller.GoalController}.
 * <p>
 * Tests the learning goals API endpoints with a real PostgreSQL database.
 */
class GoalControllerIT extends BaseIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws Exception {
        // Ensure user exists before each test
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isOk());
    }

    @Nested
    @DisplayName("POST /api/v1/goals")
    class CreateGoal {

        @Test
        @DisplayName("should create a daily goal")
        void shouldCreateDailyGoal() throws Exception {
            CreateGoalRequest request = new CreateGoalRequest(
                    "Complete 3 lessons",
                    "Study vocabulary and grammar",
                    "DAILY",
                    3,
                    "lessons"
            );

            mockMvc.perform(post("/api/v1/goals")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.title").value("Complete 3 lessons"))
                    .andExpect(jsonPath("$.description").value("Study vocabulary and grammar"))
                    .andExpect(jsonPath("$.type").value("DAILY"))
                    .andExpect(jsonPath("$.targetValue").value(3))
                    .andExpect(jsonPath("$.currentValue").value(0))
                    .andExpect(jsonPath("$.unit").value("lessons"))
                    .andExpect(jsonPath("$.completed").value(false))
                    .andExpect(jsonPath("$.progressPercent").value(0));
        }

        @Test
        @DisplayName("should create a weekly goal")
        void shouldCreateWeeklyGoal() throws Exception {
            CreateGoalRequest request = new CreateGoalRequest(
                    "Learn 50 words",
                    null,
                    "WEEKLY",
                    50,
                    "words"
            );

            mockMvc.perform(post("/api/v1/goals")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.type").value("WEEKLY"))
                    .andExpect(jsonPath("$.targetValue").value(50))
                    .andExpect(jsonPath("$.description").doesNotExist());
        }

        @Test
        @DisplayName("should create goal with lowercase type")
        void shouldCreateGoalWithLowercaseType() throws Exception {
            CreateGoalRequest request = new CreateGoalRequest(
                    "Monthly progress",
                    null,
                    "monthly",
                    100,
                    "exercises"
            );

            mockMvc.perform(post("/api/v1/goals")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.type").value("MONTHLY"));
        }

        @Test
        @DisplayName("should create all goal types")
        void shouldCreateAllGoalTypes() throws Exception {
            for (GoalType type : GoalType.values()) {
                CreateGoalRequest request = new CreateGoalRequest(
                        type.name() + " goal",
                        null,
                        type.name(),
                        10,
                        "items"
                );

                mockMvc.perform(post("/api/v1/goals")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.type").value(type.name()));
            }
        }
    }

    @Nested
    @DisplayName("GET /api/v1/goals")
    class GetGoals {

        @Test
        @DisplayName("should return all goals")
        void shouldReturnAllGoals() throws Exception {
            // Create a goal first
            createTestGoal("Test goal", "DAILY", 5, "lessons");

            mockMvc.perform(get("/api/v1/goals"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(1)));
        }

        @Test
        @DisplayName("should filter goals by type")
        void shouldFilterGoalsByType() throws Exception {
            // Create goals of different types
            createTestGoal("Daily goal", "DAILY", 3, "lessons");
            createTestGoal("Weekly goal", "WEEKLY", 10, "lessons");

            mockMvc.perform(get("/api/v1/goals").param("type", "DAILY"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[*].type", everyItem(is("DAILY"))));
        }

        @Test
        @DisplayName("should handle case-insensitive type filter")
        void shouldHandleCaseInsensitiveTypeFilter() throws Exception {
            createTestGoal("Weekly goal", "WEEKLY", 10, "lessons");

            mockMvc.perform(get("/api/v1/goals").param("type", "weekly"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[*].type", everyItem(is("WEEKLY"))));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/goals/daily/active")
    class GetActiveDailyGoals {

        @Test
        @DisplayName("should return active daily goals")
        void shouldReturnActiveDailyGoals() throws Exception {
            // Create an incomplete daily goal
            createTestGoal("Active daily goal", "DAILY", 5, "lessons");

            mockMvc.perform(get("/api/v1/goals/daily/active"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[*].type", everyItem(is("DAILY"))))
                    .andExpect(jsonPath("$[*].completed", everyItem(is(false))));
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/goals/{goalId}/progress")
    class UpdateProgress {

        @Test
        @DisplayName("should update goal progress")
        void shouldUpdateGoalProgress() throws Exception {
            String goalId = createTestGoal("Progress test", "DAILY", 10, "items");

            UpdateGoalProgressRequest request = new UpdateGoalProgressRequest(5);

            mockMvc.perform(patch("/api/v1/goals/{goalId}/progress", goalId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.currentValue").value(5))
                    .andExpect(jsonPath("$.progressPercent").value(50));
        }

        @Test
        @DisplayName("should auto-complete when target reached")
        void shouldAutoCompleteWhenTargetReached() throws Exception {
            String goalId = createTestGoal("Auto-complete test", "DAILY", 5, "items");

            UpdateGoalProgressRequest request = new UpdateGoalProgressRequest(5);

            mockMvc.perform(patch("/api/v1/goals/{goalId}/progress", goalId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.currentValue").value(5))
                    .andExpect(jsonPath("$.completed").value(true))
                    .andExpect(jsonPath("$.completedAt").isNotEmpty());
        }

        @Test
        @DisplayName("should return 404 for non-existent goal")
        void shouldReturn404ForNonExistentGoal() throws Exception {
            UUID nonExistentId = UUID.randomUUID();
            UpdateGoalProgressRequest request = new UpdateGoalProgressRequest(5);

            mockMvc.perform(patch("/api/v1/goals/{goalId}/progress", nonExistentId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/goals/{goalId}/increment")
    class IncrementProgress {

        @Test
        @DisplayName("should increment progress by default amount")
        void shouldIncrementProgressByDefaultAmount() throws Exception {
            String goalId = createTestGoal("Increment test", "DAILY", 10, "items");

            mockMvc.perform(patch("/api/v1/goals/{goalId}/increment", goalId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.currentValue").value(1))
                    .andExpect(jsonPath("$.progressPercent").value(10));
        }

        @Test
        @DisplayName("should increment progress by custom amount")
        void shouldIncrementProgressByCustomAmount() throws Exception {
            String goalId = createTestGoal("Increment test", "DAILY", 20, "items");

            mockMvc.perform(patch("/api/v1/goals/{goalId}/increment", goalId)
                            .param("amount", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.currentValue").value(5))
                    .andExpect(jsonPath("$.progressPercent").value(25));
        }

        @Test
        @DisplayName("should accumulate increments")
        void shouldAccumulateIncrements() throws Exception {
            String goalId = createTestGoal("Accumulate test", "DAILY", 10, "items");

            mockMvc.perform(patch("/api/v1/goals/{goalId}/increment", goalId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.currentValue").value(1));

            mockMvc.perform(patch("/api/v1/goals/{goalId}/increment", goalId)
                            .param("amount", "3"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.currentValue").value(4));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/goals/{goalId}/complete")
    class CompleteGoal {

        @Test
        @DisplayName("should mark goal as complete")
        void shouldMarkGoalAsComplete() throws Exception {
            String goalId = createTestGoal("Complete test", "DAILY", 10, "items");

            mockMvc.perform(post("/api/v1/goals/{goalId}/complete", goalId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.completed").value(true))
                    .andExpect(jsonPath("$.completedAt").isNotEmpty());
        }

        @Test
        @DisplayName("should set current value to target when completing")
        void shouldSetCurrentValueToTargetWhenCompleting() throws Exception {
            String goalId = createTestGoal("Complete test", "DAILY", 10, "items");

            // First increment partially
            mockMvc.perform(patch("/api/v1/goals/{goalId}/increment", goalId)
                            .param("amount", "3"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.currentValue").value(3));

            // Then complete
            mockMvc.perform(post("/api/v1/goals/{goalId}/complete", goalId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.completed").value(true))
                    .andExpect(jsonPath("$.currentValue").value(10));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/goals/{goalId}")
    class DeleteGoal {

        @Test
        @DisplayName("should delete goal")
        void shouldDeleteGoal() throws Exception {
            String goalId = createTestGoal("Delete test", "DAILY", 5, "items");

            mockMvc.perform(delete("/api/v1/goals/{goalId}", goalId))
                    .andExpect(status().isNoContent());

            // Verify it's deleted by trying to update it
            mockMvc.perform(patch("/api/v1/goals/{goalId}/increment", goalId))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 404 for non-existent goal")
        void shouldReturn404ForNonExistentGoal() throws Exception {
            UUID nonExistentId = UUID.randomUUID();

            mockMvc.perform(delete("/api/v1/goals/{goalId}", nonExistentId))
                    .andExpect(status().isNotFound());
        }
    }

    /**
     * Helper method to create a test goal and return its ID.
     */
    private String createTestGoal(String title, String type, int targetValue, String unit) throws Exception {
        CreateGoalRequest request = new CreateGoalRequest(title, null, type, targetValue, unit);

        MvcResult result = mockMvc.perform(post("/api/v1/goals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.get("id").asText();
    }
}