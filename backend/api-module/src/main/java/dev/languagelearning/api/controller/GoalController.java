package dev.languagelearning.api.controller;

import dev.languagelearning.api.dto.CreateGoalRequest;
import dev.languagelearning.api.dto.ErrorResponse;
import dev.languagelearning.api.dto.GoalDto;
import dev.languagelearning.api.dto.UpdateGoalProgressRequest;
import dev.languagelearning.core.domain.GoalType;
import dev.languagelearning.core.domain.LearningGoal;
import dev.languagelearning.learning.service.GoalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for learning goal operations.
 */
@RestController
@RequestMapping("/api/v1/goals")
@RequiredArgsConstructor
@Tag(name = "Goals", description = "Learning goals and progress tracking")
public class GoalController {

    private final GoalService goalService;

    /**
     * Gets all goals for the current user.
     */
    @GetMapping
    @Operation(
            summary = "Get all goals",
            description = "Retrieves all learning goals for the current user. Optionally filter by goal type (DAILY, WEEKLY, MONTHLY, YEARLY)."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Goals retrieved successfully",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = GoalDto.class)))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid goal type",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<List<GoalDto>> getGoals(
            @Parameter(description = "Filter goals by type", schema = @Schema(implementation = GoalType.class))
            @RequestParam(required = false) String type
    ) {
        List<LearningGoal> goals;

        if (type != null) {
            goals = goalService.getCurrentUserGoalsByType(GoalType.valueOf(type.toUpperCase()));
        } else {
            goals = goalService.getCurrentUserGoals();
        }

        return ResponseEntity.ok(goals.stream().map(GoalDto::from).toList());
    }

    /**
     * Gets active daily goals.
     */
    @GetMapping("/daily/active")
    @Operation(
            summary = "Get active daily goals",
            description = "Retrieves all active (incomplete) daily goals for the current user. Useful for displaying current day's learning objectives."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Active daily goals retrieved successfully",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = GoalDto.class)))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<List<GoalDto>> getActiveDailyGoals() {
        List<LearningGoal> goals = goalService.getActiveDailyGoals();
        return ResponseEntity.ok(goals.stream().map(GoalDto::from).toList());
    }

    /**
     * Creates a new goal.
     */
    @PostMapping
    @Operation(
            summary = "Create a new goal",
            description = "Creates a new learning goal for the current user. The goal's start and end dates are automatically calculated based on the goal type."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Goal created successfully",
                    content = @Content(schema = @Schema(implementation = GoalDto.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<GoalDto> createGoal(@RequestBody CreateGoalRequest request) {
        LearningGoal goal = goalService.createGoal(
                request.title(),
                request.description(),
                GoalType.valueOf(request.type().toUpperCase()),
                request.targetValue(),
                request.unit()
        );
        return ResponseEntity.ok(GoalDto.from(goal));
    }

    /**
     * Updates goal progress.
     */
    @PatchMapping("/{goalId}/progress")
    @Operation(
            summary = "Update goal progress",
            description = "Sets the current progress value for a goal. If the new value reaches or exceeds the target, the goal is automatically marked as complete."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Progress updated successfully",
                    content = @Content(schema = @Schema(implementation = GoalDto.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Goal not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<GoalDto> updateProgress(
            @Parameter(description = "The goal's unique identifier", required = true)
            @PathVariable UUID goalId,
            @RequestBody UpdateGoalProgressRequest request
    ) {
        LearningGoal goal = goalService.updateProgress(goalId, request.currentValue());
        return ResponseEntity.ok(GoalDto.from(goal));
    }

    /**
     * Increments goal progress.
     */
    @PatchMapping("/{goalId}/increment")
    @Operation(
            summary = "Increment goal progress",
            description = "Increments the current progress value by a specified amount. Useful for tracking incremental progress (e.g., after completing a lesson)."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Progress incremented successfully",
                    content = @Content(schema = @Schema(implementation = GoalDto.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Goal not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<GoalDto> incrementProgress(
            @Parameter(description = "The goal's unique identifier", required = true)
            @PathVariable UUID goalId,
            @Parameter(description = "Amount to increment by (default is 1)")
            @RequestParam(defaultValue = "1") int amount
    ) {
        LearningGoal goal = goalService.incrementProgress(goalId, amount);
        return ResponseEntity.ok(GoalDto.from(goal));
    }

    /**
     * Completes a goal.
     */
    @PostMapping("/{goalId}/complete")
    @Operation(
            summary = "Mark goal as complete",
            description = "Manually marks a goal as complete, regardless of current progress. Sets the completed timestamp to the current time."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Goal marked as complete",
                    content = @Content(schema = @Schema(implementation = GoalDto.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Goal not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<GoalDto> completeGoal(
            @Parameter(description = "The goal's unique identifier", required = true)
            @PathVariable UUID goalId
    ) {
        LearningGoal goal = goalService.completeGoal(goalId);
        return ResponseEntity.ok(GoalDto.from(goal));
    }

    /**
     * Deletes a goal.
     */
    @DeleteMapping("/{goalId}")
    @Operation(
            summary = "Delete a goal",
            description = "Permanently deletes a learning goal."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Goal deleted successfully"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Goal not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<Void> deleteGoal(
            @Parameter(description = "The goal's unique identifier", required = true)
            @PathVariable UUID goalId
    ) {
        goalService.deleteGoal(goalId);
        return ResponseEntity.noContent().build();
    }
}