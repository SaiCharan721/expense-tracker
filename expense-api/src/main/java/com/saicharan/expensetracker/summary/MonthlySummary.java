package com.saicharan.expensetracker.summary;

import com.saicharan.expensetracker.expense.Category;

import java.math.BigDecimal;
import java.util.List;

public record MonthlySummary(
        String month,
        BigDecimal totalSpent,
        BigDecimal totalBudget,
        long expenseCount,
        List<CategorySummary> categories) {

    public record CategorySummary(
            Category category,
            BigDecimal spent,
            long count,
            BigDecimal monthlyLimit,
            Integer percentUsed,
            BudgetStatus status) {
    }
}
