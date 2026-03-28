package dev.languagelearning.api.controller;

import dev.languagelearning.api.dto.CreateUserRequest;
import dev.languagelearning.api.dto.ErrorResponse;
import dev.languagelearning.api.dto.UpdateUserRequest;
import dev.languagelearning.api.dto.UserDto;
import dev.languagelearning.core.domain.SkillLevel;
import dev.languagelearning.core.domain.User;
import dev.languagelearning.core.exception.EntityNotFoundException;
import dev.languagelearning.learning.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for user operations.
 * <p>
 * Supports multi-user mode where the current user is determined by the
 * X-User-Id header sent with each request.
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User profile and settings management")
public class UserController {

    private final UserService userService;

    /**
     * Lists all users in the system.
     */
    @GetMapping
    @Operation(
            summary = "List all users",
            description = "Returns a list of all users in the system. Used for user selection/switching."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "List of users retrieved successfully",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = UserDto.class)))
            )
    })
    public ResponseEntity<List<UserDto>> listUsers() {
        List<User> users = userService.listAllUsers();
        List<UserDto> dtos = users.stream()
                .map(UserDto::from)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    /**
     * Creates a new user.
     */
    @PostMapping
    @Operation(
            summary = "Create a new user",
            description = "Creates a new user profile with the specified display name and language settings."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "User created successfully",
                    content = @Content(schema = @Schema(implementation = UserDto.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request (e.g., missing required fields, invalid language code)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<UserDto> createUser(@Valid @RequestBody CreateUserRequest request) {
        User user = userService.createUser(
                request.displayName(),
                request.nativeLanguage(),
                request.targetLanguage()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(UserDto.from(user));
    }

    /**
     * Gets a specific user by ID.
     */
    @GetMapping("/{userId}")
    @Operation(
            summary = "Get user by ID",
            description = "Retrieves a specific user's profile by their ID."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "User found",
                    content = @Content(schema = @Schema(implementation = UserDto.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<UserDto> getUser(
            @Parameter(description = "User ID") @PathVariable UUID userId) {
        User user = userService.findById(userId)
                .orElseThrow(() -> EntityNotFoundException.userNotFound(userId));
        return ResponseEntity.ok(UserDto.from(user));
    }

    /**
     * Gets the current user based on the X-User-Id header.
     */
    @GetMapping("/me")
    @Operation(
            summary = "Get current user",
            description = "Retrieves the current user's profile based on the X-User-Id header. " +
                    "If no user ID is provided, returns the guest user."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "User profile retrieved successfully",
                    content = @Content(schema = @Schema(implementation = UserDto.class))
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
                    description = "Invalid request (e.g., invalid skill level or language code)",
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

        // Update languages if either is provided
        String nativeLanguage = request.nativeLanguage() != null ? request.nativeLanguage() : user.getNativeLanguage();
        String targetLanguage = request.targetLanguage() != null ? request.targetLanguage() : user.getTargetLanguage();

        if (request.nativeLanguage() != null || request.targetLanguage() != null) {
            user = userService.updateLanguages(user.getId(), nativeLanguage, targetLanguage);
        }

        return ResponseEntity.ok(UserDto.from(user));
    }

    /**
     * Updates a specific user's profile.
     */
    @PutMapping("/{userId}")
    @Operation(
            summary = "Update user by ID",
            description = "Updates a specific user's profile. All fields are optional - only provided fields will be updated."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "User profile updated successfully",
                    content = @Content(schema = @Schema(implementation = UserDto.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<UserDto> updateUser(
            @Parameter(description = "User ID") @PathVariable UUID userId,
            @RequestBody UpdateUserRequest request) {

        User user = userService.findById(userId)
                .orElseThrow(() -> EntityNotFoundException.userNotFound(userId));

        if (request.displayName() != null) {
            user = userService.updateDisplayName(userId, request.displayName());
        }

        if (request.skillLevel() != null) {
            user = userService.updateSkillLevel(userId, SkillLevel.valueOf(request.skillLevel()));
        }

        String nativeLanguage = request.nativeLanguage() != null ? request.nativeLanguage() : user.getNativeLanguage();
        String targetLanguage = request.targetLanguage() != null ? request.targetLanguage() : user.getTargetLanguage();

        if (request.nativeLanguage() != null || request.targetLanguage() != null) {
            user = userService.updateLanguages(userId, nativeLanguage, targetLanguage);
        }

        return ResponseEntity.ok(UserDto.from(user));
    }

    /**
     * Deletes a user and all associated data.
     */
    @DeleteMapping("/{userId}")
    @Operation(
            summary = "Delete user",
            description = "Deletes a user and all their associated data (goals, chat sessions, exercise results, etc.)."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "User deleted successfully"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<Void> deleteUser(
            @Parameter(description = "User ID") @PathVariable UUID userId) {
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Checks if any users exist in the system.
     */
    @GetMapping("/exists")
    @Operation(
            summary = "Check if users exist",
            description = "Returns whether any users exist in the system. Useful for initial setup flow."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Check completed",
                    content = @Content(schema = @Schema(implementation = UsersExistResponse.class))
            )
    })
    public ResponseEntity<UsersExistResponse> checkUsersExist() {
        boolean hasUsers = userService.hasAnyUsers();
        return ResponseEntity.ok(new UsersExistResponse(hasUsers));
    }

    /**
     * Response DTO for users exist check.
     */
    @Schema(description = "Response indicating whether users exist")
    public record UsersExistResponse(
            @Schema(description = "True if at least one user exists in the system")
            boolean exists
    ) {
    }
}