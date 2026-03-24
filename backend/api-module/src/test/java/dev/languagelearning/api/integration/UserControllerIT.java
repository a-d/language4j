package dev.languagelearning.api.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.languagelearning.api.dto.UpdateUserRequest;
import dev.languagelearning.core.domain.SkillLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@link dev.languagelearning.api.controller.UserController}.
 * <p>
 * Tests the user API endpoints with a real PostgreSQL database.
 */
class UserControllerIT extends BaseIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @DisplayName("GET /api/v1/users/me")
    class GetCurrentUser {

        @Test
        @DisplayName("should return current user - creates default if not exists")
        void shouldReturnCurrentUser() throws Exception {
            mockMvc.perform(get("/api/v1/users/me"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.displayName").isNotEmpty())
                    .andExpect(jsonPath("$.nativeLanguage").value("de"))
                    .andExpect(jsonPath("$.targetLanguage").value("fr"))
                    .andExpect(jsonPath("$.skillLevel").value("A1"))
                    .andExpect(jsonPath("$.assessmentCompleted").value(false));
        }

        @Test
        @DisplayName("should return same user on subsequent calls")
        void shouldReturnSameUserOnSubsequentCalls() throws Exception {
            // First call - creates user
            String firstResponse = mockMvc.perform(get("/api/v1/users/me"))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            String firstUserId = objectMapper.readTree(firstResponse).get("id").asText();

            // Second call - should return same user
            mockMvc.perform(get("/api/v1/users/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(firstUserId));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/users/me")
    class UpdateCurrentUser {

        @Test
        @DisplayName("should update display name")
        void shouldUpdateDisplayName() throws Exception {
            // Ensure user exists
            mockMvc.perform(get("/api/v1/users/me"))
                    .andExpect(status().isOk());

            UpdateUserRequest request = new UpdateUserRequest("New Display Name", null);

            mockMvc.perform(put("/api/v1/users/me")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.displayName").value("New Display Name"));
        }

        @Test
        @DisplayName("should update skill level")
        void shouldUpdateSkillLevel() throws Exception {
            // Ensure user exists
            mockMvc.perform(get("/api/v1/users/me"))
                    .andExpect(status().isOk());

            UpdateUserRequest request = new UpdateUserRequest(null, "B1");

            mockMvc.perform(put("/api/v1/users/me")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.skillLevel").value("B1"));
        }

        @Test
        @DisplayName("should update both display name and skill level")
        void shouldUpdateBothFields() throws Exception {
            // Ensure user exists
            mockMvc.perform(get("/api/v1/users/me"))
                    .andExpect(status().isOk());

            UpdateUserRequest request = new UpdateUserRequest("Updated Name", "C1");

            mockMvc.perform(put("/api/v1/users/me")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.displayName").value("Updated Name"))
                    .andExpect(jsonPath("$.skillLevel").value("C1"));
        }

        @Test
        @DisplayName("should accept empty update request")
        void shouldAcceptEmptyUpdateRequest() throws Exception {
            // Ensure user exists and get current state
            String response = mockMvc.perform(get("/api/v1/users/me"))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            String currentDisplayName = objectMapper.readTree(response).get("displayName").asText();

            UpdateUserRequest request = new UpdateUserRequest(null, null);

            mockMvc.perform(put("/api/v1/users/me")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.displayName").value(currentDisplayName));
        }

        @Test
        @DisplayName("should reject invalid skill level")
        void shouldRejectInvalidSkillLevel() throws Exception {
            // Ensure user exists
            mockMvc.perform(get("/api/v1/users/me"))
                    .andExpect(status().isOk());

            UpdateUserRequest request = new UpdateUserRequest(null, "INVALID_LEVEL");

            mockMvc.perform(put("/api/v1/users/me")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should accept all valid skill levels")
        void shouldAcceptAllValidSkillLevels() throws Exception {
            // Ensure user exists
            mockMvc.perform(get("/api/v1/users/me"))
                    .andExpect(status().isOk());

            for (SkillLevel level : SkillLevel.values()) {
                UpdateUserRequest request = new UpdateUserRequest(null, level.name());

                mockMvc.perform(put("/api/v1/users/me")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.skillLevel").value(level.name()));
            }
        }
    }
}