package com.personalfinance.controller;

import com.personalfinance.dto.common.MessageResponse;
import com.personalfinance.dto.goal.GoalListResponse;
import com.personalfinance.dto.goal.GoalRequest;
import com.personalfinance.dto.goal.GoalResponse;
import com.personalfinance.dto.goal.GoalUpdateRequest;
import com.personalfinance.entity.User;
import com.personalfinance.service.AuthService;
import com.personalfinance.service.GoalService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/goals")
public class GoalController {

    private final GoalService goalService;
    private final AuthService authService;

    public GoalController(GoalService goalService, AuthService authService) {
        this.goalService = goalService;
        this.authService = authService;
    }

    @PostMapping
    public ResponseEntity<GoalResponse> createGoal(@Valid @RequestBody GoalRequest request) {
        User user = authService.getCurrentAuthenticatedUser();
        GoalResponse response = goalService.createGoal(request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<GoalListResponse> getAllGoals() {
        User user = authService.getCurrentAuthenticatedUser();
        GoalListResponse response = goalService.getAllGoals(user);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<GoalResponse> getGoalById(@PathVariable("id") Long id) {
        User user = authService.getCurrentAuthenticatedUser();
        GoalResponse response = goalService.getGoalById(id, user);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<GoalResponse> updateGoal(
            @PathVariable("id") Long id,
            @Valid @RequestBody GoalUpdateRequest request
    ) {
        User user = authService.getCurrentAuthenticatedUser();
        GoalResponse response = goalService.updateGoal(id, request, user);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> deleteGoal(@PathVariable("id") Long id) {
        User user = authService.getCurrentAuthenticatedUser();
        MessageResponse response = goalService.deleteGoal(id, user);
        return ResponseEntity.ok(response);
    }
}
