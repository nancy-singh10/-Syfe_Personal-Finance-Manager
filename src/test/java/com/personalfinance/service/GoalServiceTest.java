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
import com.personalfinance.repository.SavingsGoalRepository;
import com.personalfinance.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoalServiceTest {

    @Mock
    private SavingsGoalRepository goalRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private GoalService goalService;

    private User user;
    private SavingsGoal goal;

    @BeforeEach
    void setUp() {
        user = new User("user@example.com", "pass", "John Doe", "123");
        user.setId(1L);

        goal = new SavingsGoal(
                "Emergency Fund",
                new BigDecimal("5000.00"),
                LocalDate.now().plusMonths(6),
                LocalDate.now().minusMonths(6),
                user
        );
        goal.setId(1L);
    }

    @Test
    void testCreateGoalSuccessWithCalculatedProgress() {
        LocalDate futureTarget = LocalDate.now().plusMonths(6);
        LocalDate pastStart = LocalDate.now().minusMonths(6);

        GoalRequest req = new GoalRequest(
                "Emergency Fund",
                new BigDecimal("5000.00"),
                futureTarget,
                pastStart
        );

        Transaction inc = new Transaction(new BigDecimal("3000.00"), LocalDate.now().minusMonths(2), "Salary", "INCOME", "Salary", user);
        Transaction exp = new Transaction(new BigDecimal("2000.00"), LocalDate.now().minusMonths(1), "Rent", "EXPENSE", "Rent", user);

        when(goalRepository.save(any(SavingsGoal.class))).thenReturn(goal);
        when(transactionRepository.findByUserAndDateFrom(user, pastStart))
                .thenReturn(List.of(inc, exp));

        GoalResponse res = goalService.createGoal(req, user);

        assertNotNull(res);
        assertEquals("Emergency Fund", res.getGoalName());
        assertEquals(new BigDecimal("5000.00"), res.getTargetAmount());
        // Progress = 3000 - 2000 = 1000.00
        assertEquals(new BigDecimal("1000.00"), res.getCurrentProgress());
        // ProgressPercentage = 1000 / 5000 * 100 = 20.0
        assertEquals(20.0, res.getProgressPercentage());
        // Remaining = 5000 - 1000 = 4000.00
        assertEquals(new BigDecimal("4000.00"), res.getRemainingAmount());
    }

    @Test
    void testCreateGoalPastTargetDateThrows() {
        GoalRequest req = new GoalRequest(
                "Emergency Fund",
                new BigDecimal("5000.00"),
                LocalDate.now().minusDays(1),
                LocalDate.now()
        );

        assertThrows(BadRequestException.class, () -> goalService.createGoal(req, user));
    }

    @Test
    void testCreateGoalStartDateAfterTargetDateThrows() {
        GoalRequest req = new GoalRequest(
                "Emergency Fund",
                new BigDecimal("5000.00"),
                LocalDate.now().plusMonths(2),
                LocalDate.now().plusMonths(3)
        );

        assertThrows(BadRequestException.class, () -> goalService.createGoal(req, user));
    }

    @Test
    void testUpdateGoalSuccess() {
        LocalDate updatedTarget = LocalDate.now().plusMonths(12);
        GoalUpdateRequest req = new GoalUpdateRequest(new BigDecimal("6000.00"), updatedTarget);

        Transaction inc = new Transaction(new BigDecimal("1000.00"), LocalDate.now().minusMonths(1), "Salary", "INCOME", "Salary", user);

        when(goalRepository.findById(1L)).thenReturn(Optional.of(goal));
        when(goalRepository.save(any(SavingsGoal.class))).thenReturn(goal);
        when(transactionRepository.findByUserAndDateFrom(user, goal.getStartDate()))
                .thenReturn(List.of(inc));

        GoalResponse res = goalService.updateGoal(1L, req, user);

        assertNotNull(res);
        assertEquals(new BigDecimal("6000.00"), res.getTargetAmount());
        assertEquals(16.67, res.getProgressPercentage()); // 1000 / 6000 = 16.67%
        assertEquals(new BigDecimal("5000.00"), res.getRemainingAmount());
    }

    @Test
    void testDeleteGoalSuccess() {
        when(goalRepository.findById(1L)).thenReturn(Optional.of(goal));

        MessageResponse res = goalService.deleteGoal(1L, user);

        assertEquals("Goal deleted successfully", res.getMessage());
        verify(goalRepository).delete(goal);
    }
}
