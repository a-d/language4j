package dev.languagelearning.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.languagelearning.api.dto.UpdateUserRequest;
import dev.languagelearning.core.domain.SkillLevel;
import dev.languagelearning.core.domain.User;
import dev.languagelearning.learning.service.UserService;
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

import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for {@link UserController}.
 */
@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private User testUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController).build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        testUser = createTestUser();
    }

    @Nested
    @DisplayName("GET /api/v1/users/me")
    class GetCurrentUser {

        @Test
        @DisplayName("should return current user")
        void shouldReturnCurrentUser() throws Exception {
            when(userService.getCurrentUser()).thenReturn(testUser);

            mockMvc.perform(get("/api/v1/users/me"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(testUser.getId().toString()))
                    .andExpect(jsonPath("$.displayName").value(testUser.getDisplayName()))
                    .andExpect(jsonPath("$.nativeLanguage").value(testUser.getNativeLanguage()))
                    .andExpect(jsonPath("$.targetLanguage").value(testUser.getTargetLanguage()))
                    .andExpect(jsonPath("$.skillLevel").value(testUser.getSkillLevel().name()));

            verify(userService).getCurrentUser();
        }

        @Test
        @DisplayName("should return user with all skill levels")
        void shouldReturnUserWithAllSkillLevels() throws Exception {
            for (SkillLevel level : SkillLevel.values()) {
                User user = createTestUser();
                user.setSkillLevel(level);
                when(userService.getCurrentUser()).thenReturn(user);

                mockMvc.perform(get("/api/v1/users/me"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.skillLevel").value(level.name()));
            }
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/users/me")
    class UpdateCurrentUser {

        @Test
        @DisplayName("should update display name")
        void shouldUpdateDisplayName() throws Exception {
            UpdateUserRequest request = new UpdateUserRequest("New Name", null);
            User updatedUser = createTestUser();
            updatedUser.setDisplayName("New Name");
            
            when(userService.getCurrentUser()).thenReturn(testUser);
            when(userService.updateDisplayName(testUser.getId(), "New Name")).thenReturn(updatedUser);

            mockMvc.perform(put("/api/v1/users/me")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.displayName").value("New Name"));

            verify(userService).updateDisplayName(testUser.getId(), "New Name");
            verify(userService, never()).updateSkillLevel(any(), any());
        }

        @Test
        @DisplayName("should update skill level")
        void shouldUpdateSkillLevel() throws Exception {
            UpdateUserRequest request = new UpdateUserRequest(null, "B2");
            User updatedUser = createTestUser();
            updatedUser.setSkillLevel(SkillLevel.B2);
            
            when(userService.getCurrentUser()).thenReturn(testUser);
            when(userService.updateSkillLevel(testUser.getId(), SkillLevel.B2)).thenReturn(updatedUser);

            mockMvc.perform(put("/api/v1/users/me")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.skillLevel").value("B2"));

            verify(userService).updateSkillLevel(testUser.getId(), SkillLevel.B2);
            verify(userService, never()).updateDisplayName(any(), any());
        }

        @Test
        @DisplayName("should update both display name and skill level")
        void shouldUpdateBothDisplayNameAndSkillLevel() throws Exception {
            UpdateUserRequest request = new UpdateUserRequest("Updated Name", "C1");
            User intermediateUser = createTestUser();
            intermediateUser.setId(testUser.getId());
            intermediateUser.setDisplayName("Updated Name");
            User finalUser = createTestUser();
            finalUser.setId(testUser.getId());
            finalUser.setDisplayName("Updated Name");
            finalUser.setSkillLevel(SkillLevel.C1);
            
            when(userService.getCurrentUser()).thenReturn(testUser);
            when(userService.updateDisplayName(eq(testUser.getId()), eq("Updated Name"))).thenReturn(intermediateUser);
            when(userService.updateSkillLevel(eq(testUser.getId()), eq(SkillLevel.C1))).thenReturn(finalUser);

            mockMvc.perform(put("/api/v1/users/me")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.displayName").value("Updated Name"))
                    .andExpect(jsonPath("$.skillLevel").value("C1"));

            verify(userService).updateDisplayName(eq(testUser.getId()), eq("Updated Name"));
            verify(userService).updateSkillLevel(eq(testUser.getId()), eq(SkillLevel.C1));
        }

        @Test
        @DisplayName("should return current user when no updates provided")
        void shouldReturnCurrentUserWhenNoUpdatesProvided() throws Exception {
            UpdateUserRequest request = new UpdateUserRequest(null, null);
            when(userService.getCurrentUser()).thenReturn(testUser);

            mockMvc.perform(put("/api/v1/users/me")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(testUser.getId().toString()));

            verify(userService, never()).updateDisplayName(any(), any());
            verify(userService, never()).updateSkillLevel(any(), any());
        }

        @Test
        @DisplayName("should handle all valid skill level values")
        void shouldHandleAllValidSkillLevelValues() throws Exception {
            for (SkillLevel level : SkillLevel.values()) {
                User updatedUser = createTestUser();
                updatedUser.setSkillLevel(level);
                
                when(userService.getCurrentUser()).thenReturn(testUser);
                when(userService.updateSkillLevel(testUser.getId(), level)).thenReturn(updatedUser);

                UpdateUserRequest request = new UpdateUserRequest(null, level.name());
                
                mockMvc.perform(put("/api/v1/users/me")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.skillLevel").value(level.name()));
            }
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
}