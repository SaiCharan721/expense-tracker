package com.saicharan.expensetracker.summary;

import com.saicharan.expensetracker.expense.Category;
import com.saicharan.expensetracker.summary.MonthlySummary.CategorySummary;
import com.saicharan.expensetracker.summary.SummaryCalculator.CategoryTotal;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SummaryCalculatorTest {

    private final SummaryCalculator calculator = new SummaryCalculator(80);
    private final YearMonth october = YearMonth.of(2026, 10);

    @Test
    void marksEachCategoryAgainstItsBudget() {
        Map<Category, CategoryTotal> totals = Map.of(
                Category.FOOD, new CategoryTotal(new BigDecimal("170.50"), 6),
                Category.SHOPPING, new CategoryTotal(new BigDecimal("40.00"), 1),
                Category.TRAVEL, new CategoryTotal(new BigDecimal("320.00"), 2));
        Map<Category, BigDecimal> limits = Map.of(
                Category.FOOD, new BigDecimal("200.00"),
                Category.SHOPPING, new BigDecimal("150.00"),
                Category.TRAVEL, new BigDecimal("300.00"));

        MonthlySummary summary = calculator.build(october, totals, limits);

        assertThat(row(summary, Category.FOOD).percentUsed()).isEqualTo(85);
        assertThat(row(summary, Category.FOOD).status()).isEqualTo(BudgetStatus.WARNING);
        assertThat(row(summary, Category.SHOPPING).status()).isEqualTo(BudgetStatus.ON_TRACK);
        assertThat(row(summary, Category.TRAVEL).status()).isEqualTo(BudgetStatus.OVER_BUDGET);
        assertThat(row(summary, Category.TRAVEL).percentUsed()).isEqualTo(107);
    }

    @Test
    void includesBudgetsWithNoSpendingAndSpendingWithNoBudget() {
        MonthlySummary summary = calculator.build(october,
                Map.of(Category.HEALTH, new CategoryTotal(new BigDecimal("25"), 1)),
                Map.of(Category.GROCERIES, new BigDecimal("400.00")));

        CategorySummary groceries = row(summary, Category.GROCERIES);
        assertThat(groceries.spent()).isEqualByComparingTo("0");
        assertThat(groceries.percentUsed()).isZero();
        assertThat(groceries.status()).isEqualTo(BudgetStatus.ON_TRACK);

        CategorySummary health = row(summary, Category.HEALTH);
        assertThat(health.monthlyLimit()).isNull();
        assertThat(health.percentUsed()).isNull();
        assertThat(health.status()).isEqualTo(BudgetStatus.NO_BUDGET);
    }

    @Test
    void totalsAndSortsBiggestSpendFirst() {
        MonthlySummary summary = calculator.build(october, Map.of(
                Category.FOOD, new CategoryTotal(new BigDecimal("120.25"), 4),
                Category.RENT, new CategoryTotal(new BigDecimal("1350.00"), 1),
                Category.TRANSPORT, new CategoryTotal(new BigDecimal("45.10"), 2)),
                Map.of(Category.RENT, new BigDecimal("1350.00")));

        assertThat(summary.month()).isEqualTo("2026-10");
        assertThat(summary.totalSpent()).isEqualByComparingTo("1515.35");
        assertThat(summary.totalBudget()).isEqualByComparingTo("1350.00");
        assertThat(summary.expenseCount()).isEqualTo(7);
        assertThat(summary.categories()).extracting(CategorySummary::category)
                .containsExactly(Category.RENT, Category.FOOD, Category.TRANSPORT);
        // spending exactly the limit is a warning, not over budget
        assertThat(row(summary, Category.RENT).status()).isEqualTo(BudgetStatus.WARNING);
    }

    private static CategorySummary row(MonthlySummary summary, Category category) {
        return summary.categories().stream().filter(c -> c.category() == category).findFirst().orElseThrow();
    }
}
