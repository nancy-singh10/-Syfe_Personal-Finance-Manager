package com.personalfinance.dto.transaction;

import java.util.List;

public class TransactionListResponse {
    private List<TransactionResponse> transactions;

    public TransactionListResponse() {
    }

    public TransactionListResponse(List<TransactionResponse> transactions) {
        this.transactions = transactions;
    }

    public List<TransactionResponse> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<TransactionResponse> transactions) {
        this.transactions = transactions;
    }
}
