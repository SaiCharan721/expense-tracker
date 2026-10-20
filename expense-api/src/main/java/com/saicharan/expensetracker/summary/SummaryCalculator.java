package com.saicharan.expensetracker.summary;

import com.saicharan.expensetracker.expense.Category;
import com.saicharan.expensetracker.summary.MonthlySummary.CategorySummary;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Pure logic that merges what was spent with the budgets for the month.
 * Kept separate from the Mongo aggregation so it is easy to unit test.
 */
public class SummaryCalculator {

    public record CategoryTotal(BigDecimal spent, long count) {
    }

    private final int warningPercent;

    public SummaryCalculator(int warningPercent) {
        this.warningPercent = warningPercent;
    }

    public MonthlySummary build(YearMonth month, Map<Category, CategoryTotal> totals, Map<Category, BigDecimal> limits) {
        Set<Category> categories = EnumSet.noneOf(Category.class);
        categories.addAll(totals.keySet());
        categories.addAll(limits.keySet());

        List<CategorySummary> rows = new ArrayList<>();
        BigDecimal totalSpent = BigDecimal.ZERO;
        BigDecimal totalBudget = BigDecimal.ZERO;
        long count = 0;

        for (Category category : categories) {
            CategoryTotal total = totals.getOrDefault(category, new CategoryTotal(BigDecimal.ZERO, 0));
            BigDecimal spent = total.spent().setScale(2, RoundingMode.HALF_UP);
            BigDecimal limit = limits.get(category);

            Integer percent = null;
            BudgetStatus status = BudgetStatus.NO_BUDGET;
            if (limit != null && limit.signum() > 0) {
                percent = spent.multiply(BigDecimal.valueOf(100)).divide(limit, 0, RoundingMode.HALF_UP).intValue();
                status = statusFor(spent, limit, percent);
                totalBudget = totalBudget.add(limit);
            }

            rows.add(new CategorySummary(category, spent, total.count(), limit, percent, status));
            totalSpent = totalSpent.add(spent);
            count += total.count();
        }

        rows.sort(Comparator.comparing(CategorySummary::spent).reversed()
                .thenComparing(CategorySummary::category));

        return new MonthlySummary(month.toString(), totalSpent.setScale(2, RoundingMode.HALF_UP),
                totalBudget.setScale(2, RoundingMode.HALF_UP), count, rows);
    }

    private BudgetStatus statusFor(BigDecimal spent, BigDecimal limit, int percent) {
        if (spent.compareTo(limit) > 0) {
            return BudgetStatus.OVER_BUDGET;
        }
        return percent >= warningPercent ? BudgetStatus.WARNING : BudgetStatus.ON_TRACK;
    }
}
