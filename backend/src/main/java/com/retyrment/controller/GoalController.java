package com.retyrment.controller;

import com.retyrment.exception.ResourceNotFoundException;
import com.retyrment.model.Goal;
import com.retyrment.model.Goal.Priority;
import com.retyrment.repository.GoalRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/goals")
@RequiredArgsConstructor
public class GoalController extends BaseController {

    private final GoalRepository goalRepository;

    @GetMapping
    public List<Goal> getAllGoals() {
        String userId = getCurrentUserId();
        return goalRepository.findByUserIdOrderByTargetYearAsc(userId);
    }

    @GetMapping("/priority/{priority}")
    public List<Goal> getGoalsByPriority(@PathVariable Priority priority) {
        String userId = getCurrentUserId();
        return goalRepository.findByUserId(userId).stream()
                .filter(g -> g.getPriority() == priority)
                .toList();
    }

    @GetMapping("/recurring")
    public List<Goal> getRecurringGoals() {
        String userId = getCurrentUserId();
        return goalRepository.findByUserIdAndIsRecurringTrue(userId);
    }

    @GetMapping("/upcoming/{year}")
    public List<Goal> getUpcomingGoals(@PathVariable Integer year) {
        String userId = getCurrentUserId();
        return goalRepository.findByUserId(userId).stream()
                .filter(g -> g.getTargetYear() != null && g.getTargetYear() <= year)
                .toList();
    }

    @GetMapping("/{id}")
    public Goal getGoalById(@PathVariable String id) {
        String userId = getCurrentUserId();
        return goalRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Goal", id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Goal createGoal(@Valid @RequestBody Goal goal) {
        String userId = getCurrentUserId();
        goal.setUserId(userId);
        goal.setIsRecurring(goal.getIsRecurring() != null ? goal.getIsRecurring() : false);
        goal.setPriority(goal.getPriority() != null ? goal.getPriority() : Priority.MEDIUM);
        return goalRepository.save(goal);
    }

    @PutMapping("/{id}")
    public Goal updateGoal(@PathVariable String id, @Valid @RequestBody Goal goal) {
        String userId = getCurrentUserId();
        Goal existing = goalRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Goal", id));
        goal.setId(id);
        goal.setUserId(userId); // Ensure userId cannot be changed
        return goalRepository.save(goal);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteGoal(@PathVariable String id) {
        String userId = getCurrentUserId();
        if (!goalRepository.existsByIdAndUserId(id, userId)) {
            throw new ResourceNotFoundException("Goal", id);
        }
        goalRepository.deleteById(id);
    }
}
