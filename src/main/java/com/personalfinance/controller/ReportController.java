package com.personalfinance.controller;

import com.personalfinance.dto.report.MonthlyReportResponse;
import com.personalfinance.dto.report.YearlyReportResponse;
import com.personalfinance.entity.User;
import com.personalfinance.service.AuthService;
import com.personalfinance.service.ReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;
    private final AuthService authService;

    public ReportController(ReportService reportService, AuthService authService) {
        this.reportService = reportService;
        this.authService = authService;
    }

    @GetMapping("/monthly/{year}/{month}")
    public ResponseEntity<MonthlyReportResponse> getMonthlyReport(
            @PathVariable("year") int year,
            @PathVariable("month") int month
    ) {
        User user = authService.getCurrentAuthenticatedUser();
        MonthlyReportResponse response = reportService.getMonthlyReport(year, month, user);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/yearly/{year}")
    public ResponseEntity<YearlyReportResponse> getYearlyReport(
            @PathVariable("year") int year
    ) {
        User user = authService.getCurrentAuthenticatedUser();
        YearlyReportResponse response = reportService.getYearlyReport(year, user);
        return ResponseEntity.ok(response);
    }
}
