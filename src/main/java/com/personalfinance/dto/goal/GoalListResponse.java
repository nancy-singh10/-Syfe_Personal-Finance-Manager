package com.personalfinance.dto.goal;

import java.util.List;

public class GoalListResponse {
    private List<GoalResponse> goals;

    public GoalListResponse() {
    }

    public GoalListResponse(List<GoalResponse> goals) {
        this.goals = goals;
    }

    public List<GoalResponse> getGoals() {
        return goals;
    }

    public void setGoals(List<GoalResponse> goals) {
        this.goals = goals;
    }
}
