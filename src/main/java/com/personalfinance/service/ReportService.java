package com.personalfinance.service;

import com.personalfinance.dto.report.MonthlyReportResponse;
import com.personalfinance.dto.report.YearlyReportResponse;
import com.personalfinance.entity.Transaction;
import com.personalfinance.entity.User;
import com.personalfinance.exception.BadRequestException;
import com.personalfinance.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportService {

    private final TransactionRepository transactionRepository;

    public ReportService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Transactional(readOnly = true)
    public MonthlyReportResponse getMonthlyReport(int year, int month, User user) {
        if (month < 1 || month > 12) {
            throw new BadRequestException("Month must be between 1 and 12");
        }
        if (year < 1900 || year > 2100) {
            throw new BadRequestException("Year must be a valid 4-digit year");
        }

        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        List<Transaction> transactions = transactionRepository.findByUserAndDateRange(user, startDate, endDate);

        Map<String, BigDecimal> totalIncome = new LinkedHashMap<>();
        Map<String, BigDecimal> totalExpenses = new LinkedHashMap<>();
        BigDecimal sumIncome = BigDecimal.ZERO;
        BigDecimal sumExpenses = BigDecimal.ZERO;

        for (Transaction t : transactions) {
            BigDecimal amt = t.getAmount().setScale(2, RoundingMode.HALF_UP);
            if ("INCOME".equalsIgnoreCase(t.getType())) {
                totalIncome.merge(t.getCategory(), amt, BigDecimal::add);
                sumIncome = sumIncome.add(amt);
            } else if ("EXPENSE".equalsIgnoreCase(t.getType())) {
                totalExpenses.merge(t.getCategory(), amt, BigDecimal::add);
                sumExpenses = sumExpenses.add(amt);
            }
        }

        BigDecimal netSavings = sumIncome.subtract(sumExpenses).setScale(2, RoundingMode.HALF_UP);

        return new MonthlyReportResponse(month, year, totalIncome, totalExpenses, netSavings);
    }

    @Transactional(readOnly = true)
    public YearlyReportResponse getYearlyReport(int year, User user) {
        if (year < 1900 || year > 2100) {
            throw new BadRequestException("Year must be a valid 4-digit year");
        }

        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);

        List<Transaction> transactions = transactionRepository.findByUserAndDateRange(user, startDate, endDate);

        Map<String, BigDecimal> totalIncome = new LinkedHashMap<>();
        Map<String, BigDecimal> totalExpenses = new LinkedHashMap<>();
        BigDecimal sumIncome = BigDecimal.ZERO;
        BigDecimal sumExpenses = BigDecimal.ZERO;

        for (Transaction t : transactions) {
            BigDecimal amt = t.getAmount().setScale(2, RoundingMode.HALF_UP);
            if ("INCOME".equalsIgnoreCase(t.getType())) {
                totalIncome.merge(t.getCategory(), amt, BigDecimal::add);
                sumIncome = sumIncome.add(amt);
            } else if ("EXPENSE".equalsIgnoreCase(t.getType())) {
                totalExpenses.merge(t.getCategory(), amt, BigDecimal::add);
                sumExpenses = sumExpenses.add(amt);
            }
        }

        BigDecimal netSavings = sumIncome.subtract(sumExpenses).setScale(2, RoundingMode.HALF_UP);

        return new YearlyReportResponse(year, totalIncome, totalExpenses, netSavings);
    }
}
