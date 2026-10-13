package com.personalfinance.service;

import com.personalfinance.dto.report.MonthlyReportResponse;
import com.personalfinance.dto.report.YearlyReportResponse;
import com.personalfinance.entity.Transaction;
import com.personalfinance.entity.User;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private ReportService reportService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User("user@example.com", "pass", "John Doe", "123");
        user.setId(1L);
    }

    @Test
    void testMonthlyReportCalculation() {
        Transaction inc1 = new Transaction(new BigDecimal("3000.00"), LocalDate.of(2024, 1, 15), "Salary", "INCOME", "Salary", user);
        Transaction inc2 = new Transaction(new BigDecimal("500.00"), LocalDate.of(2024, 1, 20), "Freelance", "INCOME", "Side", user);
        Transaction exp1 = new Transaction(new BigDecimal("400.00"), LocalDate.of(2024, 1, 5), "Food", "EXPENSE", "Groceries", user);
        Transaction exp2 = new Transaction(new BigDecimal("1200.00"), LocalDate.of(2024, 1, 1), "Rent", "EXPENSE", "Rent", user);
        Transaction exp3 = new Transaction(new BigDecimal("200.00"), LocalDate.of(2024, 1, 18), "Transportation", "EXPENSE", "Bus", user);

        when(transactionRepository.findByUserAndDateRange(user, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31)))
                .thenReturn(List.of(inc1, inc2, exp1, exp2, exp3));

        MonthlyReportResponse report = reportService.getMonthlyReport(2024, 1, user);

        assertNotNull(report);
        assertEquals(1, report.getMonth());
        assertEquals(2024, report.getYear());

        assertEquals(new BigDecimal("3000.00"), report.getTotalIncome().get("Salary"));
        assertEquals(new BigDecimal("500.00"), report.getTotalIncome().get("Freelance"));

        assertEquals(new BigDecimal("400.00"), report.getTotalExpenses().get("Food"));
        assertEquals(new BigDecimal("1200.00"), report.getTotalExpenses().get("Rent"));
        assertEquals(new BigDecimal("200.00"), report.getTotalExpenses().get("Transportation"));

        // Net savings = (3000 + 500) - (400 + 1200 + 200) = 3500 - 1800 = 1700.00
        assertEquals(new BigDecimal("1700.00"), report.getNetSavings());
    }

    @Test
    void testYearlyReportCalculation() {
        Transaction inc = new Transaction(new BigDecimal("36000.00"), LocalDate.of(2024, 6, 15), "Salary", "INCOME", "Salary", user);
        Transaction exp = new Transaction(new BigDecimal("14400.00"), LocalDate.of(2024, 6, 1), "Rent", "EXPENSE", "Rent", user);

        when(transactionRepository.findByUserAndDateRange(user, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31)))
                .thenReturn(List.of(inc, exp));

        YearlyReportResponse report = reportService.getYearlyReport(2024, user);

        assertNotNull(report);
        assertEquals(2024, report.getYear());
        assertEquals(new BigDecimal("36000.00"), report.getTotalIncome().get("Salary"));
        assertEquals(new BigDecimal("14400.00"), report.getTotalExpenses().get("Rent"));
        assertEquals(new BigDecimal("21600.00"), report.getNetSavings());
    }
}
