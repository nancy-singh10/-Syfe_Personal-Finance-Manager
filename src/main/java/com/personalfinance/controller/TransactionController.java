package com.personalfinance.controller;

import com.personalfinance.dto.common.MessageResponse;
import com.personalfinance.dto.transaction.TransactionListResponse;
import com.personalfinance.dto.transaction.TransactionRequest;
import com.personalfinance.dto.transaction.TransactionResponse;
import com.personalfinance.dto.transaction.TransactionUpdateRequest;
import com.personalfinance.entity.User;
import com.personalfinance.service.AuthService;
import com.personalfinance.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final AuthService authService;

    public TransactionController(TransactionService transactionService, AuthService authService) {
        this.transactionService = transactionService;
        this.authService = authService;
    }

    @PostMapping
    public ResponseEntity<TransactionResponse> createTransaction(@Valid @RequestBody TransactionRequest request) {
        User user = authService.getCurrentAuthenticatedUser();
        TransactionResponse response = transactionService.createTransaction(request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<TransactionListResponse> getTransactions(
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(name = "category", required = false) String category,
            @RequestParam(name = "type", required = false) String type
    ) {
        User user = authService.getCurrentAuthenticatedUser();
        TransactionListResponse response = transactionService.getTransactions(user, startDate, endDate, category, type);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TransactionResponse> updateTransaction(
            @PathVariable("id") Long id,
            @Valid @RequestBody TransactionUpdateRequest request
    ) {
        User user = authService.getCurrentAuthenticatedUser();
        TransactionResponse response = transactionService.updateTransaction(id, request, user);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> deleteTransaction(@PathVariable("id") Long id) {
        User user = authService.getCurrentAuthenticatedUser();
        MessageResponse response = transactionService.deleteTransaction(id, user);
        return ResponseEntity.ok(response);
    }
}
