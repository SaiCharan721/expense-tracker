package com.saicharan.expensetracker.expense;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ExpenseRepository extends MongoRepository<Expense, String> {

    Optional<Expense> findByIdAndUserId(String id, String userId);

    long deleteByIdAndUserId(String id, String userId);
}
