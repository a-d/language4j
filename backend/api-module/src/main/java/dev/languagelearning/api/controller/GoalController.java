package dev.languagelearning.api.controller;

import dev.languagelearning.api.dto.CreateGoalRequest;
import dev.languagelearning.api.dto.GoalDto;
import dev.languagelearning.api.dto.UpdateGoalProgressRequest;
import dev.languagelearning.core.domain.GoalType;
import dev.languagelearning.core.domain.LearningGoal;
import dev.languagelearning.learning.service.GoalService;
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
public class GoalController {

    private final GoalService goalService;

    /**
     * Gets all goals for the current user.
     */
    @GetMapping
    public ResponseEntity<List<GoalDto>> getGoals(@RequestParam(required = false) String type) {
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
    public ResponseEntity<List<GoalDto>> getActiveDailyGoals() {
        List<LearningGoal> goals = goalService.getActiveDailyGoals();
        return ResponseEntity.ok(goals.stream().map(GoalDto::from).toList());
    }

    /**
     * Creates a new goal.
     */
    @PostMapping
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
    public ResponseEntity<GoalDto> updateProgress(
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
    public ResponseEntity<GoalDto> incrementProgress(
            @PathVariable UUID goalId,
            @RequestParam(defaultValue = "1") int amount
    ) {
        LearningGoal goal = goalService.incrementProgress(goalId, amount);
        return ResponseEntity.ok(GoalDto.from(goal));
    }

    /**
     * Completes a goal.
     */
    @PostMapping("/{goalId}/complete")
    public ResponseEntity<GoalDto> completeGoal(@PathVariable UUID goalId) {
        LearningGoal goal = goalService.completeGoal(goalId);
        return ResponseEntity.ok(GoalDto.from(goal));
    }

    /**
     * Deletes a goal.
     */
    @DeleteMapping("/{goalId}")
    public ResponseEntity<Void> deleteGoal(@PathVariable UUID goalId) {
        goalService.deleteGoal(goalId);
        return ResponseEntity.noContent().build();
    }
}