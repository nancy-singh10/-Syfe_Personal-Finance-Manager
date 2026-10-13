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
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CategoryService categoryService;

    @InjectMocks
    private TransactionService transactionService;

    private User user;
    private User otherUser;
    private Category category;
    private Transaction transaction;

    @BeforeEach
    void setUp() {
        user = new User("user@example.com", "pass", "John Doe", "123");
        user.setId(1L);

        otherUser = new User("other@example.com", "pass", "Jane", "456");
        otherUser.setId(2L);

        category = new Category("Salary", "INCOME", false, null);

        transaction = new Transaction(
                new BigDecimal("50000.00"),
                LocalDate.of(2024, 1, 15),
                "Salary",
                "INCOME",
                "January Salary",
                user
        );
        transaction.setId(100L);
    }

    @Test
    void testCreateTransactionSuccess() {
        TransactionRequest req = new TransactionRequest(
                new BigDecimal("50000.00"),
                LocalDate.of(2024, 1, 15),
                "Salary",
                "January Salary"
        );

        when(categoryService.findAccessibleCategoryByName("Salary", user)).thenReturn(category);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);

        TransactionResponse res = transactionService.createTransaction(req, user);

        assertNotNull(res);
        assertEquals(new BigDecimal("50000.00"), res.getAmount());
        assertEquals(LocalDate.of(2024, 1, 15), res.getDate());
        assertEquals("Salary", res.getCategory());
        assertEquals("INCOME", res.getType());
    }

    @Test
    void testCreateTransactionFutureDateThrows() {
        TransactionRequest req = new TransactionRequest(
                new BigDecimal("100.00"),
                LocalDate.now().plusDays(5),
                "Salary",
                "Future Salary"
        );

        assertThrows(BadRequestException.class, () -> transactionService.createTransaction(req, user));
    }

    @Test
    void testGetTransactions() {
        when(transactionRepository.findFilteredTransactions(user, null, null, null, null))
                .thenReturn(List.of(transaction));

        TransactionListResponse res = transactionService.getTransactions(user, null, null, null, null);

        assertNotNull(res);
        assertEquals(1, res.getTransactions().size());
        assertEquals("Salary", res.getTransactions().get(0).getCategory());
    }

    @Test
    void testUpdateTransactionAmountAndDescription() {
        TransactionUpdateRequest req = new TransactionUpdateRequest(new BigDecimal("60000.00"), "Updated Salary");

        when(transactionRepository.findById(100L)).thenReturn(Optional.of(transaction));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);

        TransactionResponse res = transactionService.updateTransaction(100L, req, user);

        assertNotNull(res);
        verify(transactionRepository).save(transaction);
        assertEquals(new BigDecimal("60000.00"), transaction.getAmount());
        assertEquals("Updated Salary", transaction.getDescription());
    }

    @Test
    void testUpdateTransactionDateAttemptThrows() {
        TransactionUpdateRequest req = new TransactionUpdateRequest(new BigDecimal("60000.00"), "Updated");
        req.setDate(LocalDate.of(2024, 2, 20)); // Changing date from 2024-01-15

        when(transactionRepository.findById(100L)).thenReturn(Optional.of(transaction));

        assertThrows(BadRequestException.class, () -> transactionService.updateTransaction(100L, req, user));
    }

    @Test
    void testUpdateOtherUserTransactionThrowsForbidden() {
        TransactionUpdateRequest req = new TransactionUpdateRequest(new BigDecimal("60000.00"), "Updated");

        when(transactionRepository.findById(100L)).thenReturn(Optional.of(transaction));

        assertThrows(ForbiddenException.class, () -> transactionService.updateTransaction(100L, req, otherUser));
    }

    @Test
    void testDeleteTransactionSuccess() {
        when(transactionRepository.findById(100L)).thenReturn(Optional.of(transaction));

        MessageResponse res = transactionService.deleteTransaction(100L, user);

        assertEquals("Transaction deleted successfully", res.getMessage());
        verify(transactionRepository).delete(transaction);
    }
}
