package dev.languagelearning.api.controller;

import dev.languagelearning.api.dto.ErrorResponse;
import dev.languagelearning.api.dto.UpdateUserRequest;
import dev.languagelearning.api.dto.UserDto;
import dev.languagelearning.core.domain.SkillLevel;
import dev.languagelearning.core.domain.User;
import dev.languagelearning.learning.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for user operations.
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User profile and settings management")
public class UserController {

    private final UserService userService;

    /**
     * Gets the current user.
     */
    @GetMapping("/me")
    @Operation(
            summary = "Get current user",
            description = "Retrieves the current user's profile information. In single-user mode, this creates a default user if none exists."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "User profile retrieved successfully",
                    content = @Content(schema = @Schema(implementation = UserDto.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<UserDto> getCurrentUser() {
        User user = userService.getCurrentUser();
        return ResponseEntity.ok(UserDto.from(user));
    }

    /**
     * Updates the current user's profile.
     */
    @PutMapping("/me")
    @Operation(
            summary = "Update current user",
            description = "Updates the current user's profile information. All fields are optional - only provided fields will be updated."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "User profile updated successfully",
                    content = @Content(schema = @Schema(implementation = UserDto.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request (e.g., invalid skill level)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<UserDto> updateCurrentUser(@RequestBody UpdateUserRequest request) {
        User user = userService.getCurrentUser();

        if (request.displayName() != null) {
            user = userService.updateDisplayName(user.getId(), request.displayName());
        }

        if (request.skillLevel() != null) {
            user = userService.updateSkillLevel(user.getId(), SkillLevel.valueOf(request.skillLevel()));
        }

        return ResponseEntity.ok(UserDto.from(user));
    }
}