package com.personalfinance.service;

import com.personalfinance.dto.common.MessageResponse;
import com.personalfinance.dto.transaction.TransactionListResponse;
import com.personalfinance.dto.transaction.TransactionRequest;
import com.personalfinance.dto.transaction.TransactionResponse;
import com.personalfinance.dto.transaction.TransactionUpdateRequest;
import com.personalfinance.entity.Category;
import com.personalfinance.entity.Transaction;
import com.personalfinance.entity.User;
import com.personalfinance.exception.BadRequestException;
import com.personalfinance.exception.ForbiddenException;
import com.personalfinance.exception.ResourceNotFoundException;
import com.personalfinance.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CategoryService categoryService;

    public TransactionService(TransactionRepository transactionRepository, CategoryService categoryService) {
        this.transactionRepository = transactionRepository;
        this.categoryService = categoryService;
    }

    @Transactional
    public TransactionResponse createTransaction(TransactionRequest request, User user) {
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Amount must be a positive decimal value");
        }

        if (request.getDate() == null) {
            throw new BadRequestException("Transaction date is required");
        }

        if (request.getDate().isAfter(LocalDate.now())) {
            throw new BadRequestException("Transaction date cannot be a future date");
        }

        Category category = categoryService.findAccessibleCategoryByName(request.getCategory(), user);

        Transaction transaction = new Transaction(
                request.getAmount(),
                request.getDate(),
                category.getName(),
                category.getType(),
                request.getDescription(),
                user
        );

        Transaction saved = transactionRepository.save(transaction);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public TransactionListResponse getTransactions(User user, LocalDate startDate, LocalDate endDate, String category, String type) {
        List<Transaction> transactions = transactionRepository.findFilteredTransactions(user, startDate, endDate, category, type);
        List<TransactionResponse> responses = new ArrayList<>();
        for (Transaction t : transactions) {
            responses.add(mapToResponse(t));
        }
        return new TransactionListResponse(responses);
    }

    @Transactional
    public TransactionResponse updateTransaction(Long id, TransactionUpdateRequest request, User user) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + id));

        if (!transaction.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("You are not authorized to update this transaction");
        }

        // Rule: Users can modify any transaction field except the date field.
        if (request.getDate() != null && !request.getDate().equals(transaction.getDate())) {
            throw new BadRequestException("Transaction date cannot be modified");
        }

        if (request.getAmount() != null) {
            if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("Amount must be a positive decimal value");
            }
            transaction.setAmount(request.getAmount());
        }

        if (request.getDescription() != null) {
            transaction.setDescription(request.getDescription());
        }

        if (request.getCategory() != null && !request.getCategory().isBlank()) {
            Category category = categoryService.findAccessibleCategoryByName(request.getCategory(), user);
            transaction.setCategory(category.getName());
            transaction.setType(category.getType());
        }

        Transaction updated = transactionRepository.save(transaction);
        return mapToResponse(updated);
    }

    @Transactional
    public MessageResponse deleteTransaction(Long id, User user) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + id));

        if (!transaction.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("You are not authorized to delete this transaction");
        }

        transactionRepository.delete(transaction);
        return new MessageResponse("Transaction deleted successfully");
    }

    private TransactionResponse mapToResponse(Transaction t) {
        return new TransactionResponse(
                t.getId(),
                t.getAmount(),
                t.getDate(),
                t.getCategory(),
                t.getDescription(),
                t.getType()
        );
    }
}
