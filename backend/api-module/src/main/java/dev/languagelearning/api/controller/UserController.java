package dev.languagelearning.api.controller;

import dev.languagelearning.api.dto.UserDto;
import dev.languagelearning.api.dto.UpdateUserRequest;
import dev.languagelearning.core.domain.SkillLevel;
import dev.languagelearning.core.domain.User;
import dev.languagelearning.learning.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for user operations.
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * Gets the current user.
     */
    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser() {
        User user = userService.getCurrentUser();
        return ResponseEntity.ok(UserDto.from(user));
    }

    /**
     * Updates the current user's profile.
     */
    @PutMapping("/me")
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