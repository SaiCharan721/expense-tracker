package com.saicharan.expensetracker.budget;

import com.saicharan.expensetracker.expense.Category;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends MongoRepository<Budget, String> {

    List<Budget> findByUserIdOrderByCategory(String userId);

    Optional<Budget> findByUserIdAndCategory(String userId, Category category);

    long deleteByUserIdAndCategory(String userId, Category category);
}
