package com.personalfinance.service;

import com.personalfinance.dto.common.MessageResponse;
import com.personalfinance.dto.goal.GoalListResponse;
import com.personalfinance.dto.goal.GoalRequest;
import com.personalfinance.dto.goal.GoalResponse;
import com.personalfinance.dto.goal.GoalUpdateRequest;
import com.personalfinance.entity.SavingsGoal;
import com.personalfinance.entity.Transaction;
import com.personalfinance.entity.User;
import com.personalfinance.exception.BadRequestException;
import com.personalfinance.exception.ForbiddenException;
import com.personalfinance.exception.ResourceNotFoundException;
import com.personalfinance.repository.SavingsGoalRepository;
import com.personalfinance.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class GoalService {

    private final SavingsGoalRepository goalRepository;
    private final TransactionRepository transactionRepository;

    public GoalService(SavingsGoalRepository goalRepository, TransactionRepository transactionRepository) {
        this.goalRepository = goalRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public GoalResponse createGoal(GoalRequest request, User user) {
        if (request.getTargetAmount() == null || request.getTargetAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Target amount must be a positive decimal value");
        }

        if (request.getTargetDate() == null) {
            throw new BadRequestException("Target date is required");
        }

        if (!request.getTargetDate().isAfter(LocalDate.now())) {
            throw new BadRequestException("Target date must be a future date");
        }

        LocalDate startDate = request.getStartDate() != null ? request.getStartDate() : LocalDate.now();

        if (startDate.isAfter(request.getTargetDate())) {
            throw new BadRequestException("Start date cannot be after target date");
        }

        SavingsGoal goal = new SavingsGoal(
                request.getGoalName(),
                request.getTargetAmount(),
                request.getTargetDate(),
                startDate,
                user
        );

        SavingsGoal saved = goalRepository.save(goal);
        return calculateAndMap(saved, user);
    }

    @Transactional(readOnly = true)
    public GoalListResponse getAllGoals(User user) {
        List<SavingsGoal> goals = goalRepository.findByUserOrderByIdAsc(user);
        List<GoalResponse> responses = new ArrayList<>();
        for (SavingsGoal goal : goals) {
            responses.add(calculateAndMap(goal, user));
        }
        return new GoalListResponse(responses);
    }

    @Transactional(readOnly = true)
    public GoalResponse getGoalById(Long id, User user) {
        SavingsGoal goal = goalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Goal not found with id: " + id));

        if (!goal.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("You are not authorized to view this goal");
        }

        return calculateAndMap(goal, user);
    }

    @Transactional
    public GoalResponse updateGoal(Long id, GoalUpdateRequest request, User user) {
        SavingsGoal goal = goalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Goal not found with id: " + id));

        if (!goal.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("You are not authorized to update this goal");
        }

        if (request.getTargetAmount() != null) {
            if (request.getTargetAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("Target amount must be a positive decimal value");
            }
            goal.setTargetAmount(request.getTargetAmount());
        }

        if (request.getTargetDate() != null) {
            if (!request.getTargetDate().isAfter(LocalDate.now())) {
                throw new BadRequestException("Target date must be a future date");
            }
            if (goal.getStartDate() != null && request.getTargetDate().isBefore(goal.getStartDate())) {
                throw new BadRequestException("Target date cannot be before goal start date");
            }
            goal.setTargetDate(request.getTargetDate());
        }

        SavingsGoal updated = goalRepository.save(goal);
        return calculateAndMap(updated, user);
    }

    @Transactional
    public MessageResponse deleteGoal(Long id, User user) {
        SavingsGoal goal = goalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Goal not found with id: " + id));

        if (!goal.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("You are not authorized to delete this goal");
        }

        goalRepository.delete(goal);
        return new MessageResponse("Goal deleted successfully");
    }

    private GoalResponse calculateAndMap(SavingsGoal goal, User user) {
        // Calculation: (Total Income - Total Expenses) since goal start date
        List<Transaction> transactions = transactionRepository.findByUserAndDateFrom(user, goal.getStartDate());

        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpenses = BigDecimal.ZERO;

        for (Transaction t : transactions) {
            if ("INCOME".equalsIgnoreCase(t.getType())) {
                totalIncome = totalIncome.add(t.getAmount());
            } else if ("EXPENSE".equalsIgnoreCase(t.getType())) {
                totalExpenses = totalExpenses.add(t.getAmount());
            }
        }

        BigDecimal currentProgress = totalIncome.subtract(totalExpenses);

        BigDecimal target = goal.getTargetAmount();
        double progressPercentage = 0.0;
        if (target.compareTo(BigDecimal.ZERO) > 0 && currentProgress.compareTo(BigDecimal.ZERO) > 0) {
            double calculated = (currentProgress.doubleValue() / target.doubleValue()) * 100.0;
            // Round to 2 decimal places e.g., 60.33
            progressPercentage = Math.round(calculated * 100.0) / 100.0;
        }

        BigDecimal remainingAmount = target.subtract(currentProgress);
        if (remainingAmount.compareTo(BigDecimal.ZERO) < 0) {
            remainingAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        } else {
            remainingAmount = remainingAmount.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal progressToReturn = currentProgress.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : currentProgress.setScale(2, RoundingMode.HALF_UP);

        return new GoalResponse(
                goal.getId(),
                goal.getGoalName(),
                goal.getTargetAmount().setScale(2, RoundingMode.HALF_UP),
                goal.getTargetDate(),
                goal.getStartDate(),
                progressToReturn,
                progressPercentage,
                remainingAmount
        );
    }
}
