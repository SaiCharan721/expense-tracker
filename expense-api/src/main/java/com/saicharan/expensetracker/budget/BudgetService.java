package com.saicharan.expensetracker.budget;

import com.saicharan.expensetracker.common.NotFoundException;
import com.saicharan.expensetracker.expense.Category;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class BudgetService {

    private final BudgetRepository budgets;

    public BudgetService(BudgetRepository budgets) {
        this.budgets = budgets;
    }

    public List<BudgetResponse> list(String userId) {
        return budgets.findByUserIdOrderByCategory(userId).stream().map(BudgetResponse::from).toList();
    }

    /** Creates the budget or replaces the limit if one already exists for that category. */
    public BudgetResponse upsert(String userId, Category category, BigDecimal limit) {
        BigDecimal scaled = limit.setScale(2, RoundingMode.HALF_UP);
        Budget budget = budgets.findByUserIdAndCategory(userId, category)
                .map(existing -> {
                    existing.setMonthlyLimit(scaled);
                    return existing;
                })
                .orElseGet(() -> new Budget(userId, category, scaled));
        return BudgetResponse.from(budgets.save(budget));
    }

    public void delete(String userId, Category category) {
        if (budgets.deleteByUserIdAndCategory(userId, category) == 0) {
            throw new NotFoundException("No budget set for " + category);
        }
    }

    public record BudgetResponse(Category category, BigDecimal monthlyLimit) {
        static BudgetResponse from(Budget b) {
            return new BudgetResponse(b.getCategory(), b.getMonthlyLimit());
        }
    }
}
