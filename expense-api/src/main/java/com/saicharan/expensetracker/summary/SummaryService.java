package com.saicharan.expensetracker.summary;

import com.saicharan.expensetracker.budget.Budget;
import com.saicharan.expensetracker.budget.BudgetRepository;
import com.saicharan.expensetracker.expense.Category;
import com.saicharan.expensetracker.expense.Expense;
import com.saicharan.expensetracker.summary.SummaryCalculator.CategoryTotal;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.EnumMap;
import java.util.Map;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.group;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.match;
import static org.springframework.data.mongodb.core.query.Criteria.where;

@Service
public class SummaryService {

    private final MongoTemplate mongoTemplate;
    private final BudgetRepository budgets;
    private final SummaryCalculator calculator;

    public SummaryService(MongoTemplate mongoTemplate, BudgetRepository budgets,
                          @Value("${app.budget.warning-percent:80}") int warningPercent) {
        this.mongoTemplate = mongoTemplate;
        this.budgets = budgets;
        this.calculator = new SummaryCalculator(warningPercent);
    }

    public MonthlySummary monthly(String userId, YearMonth month) {
        Aggregation aggregation = Aggregation.newAggregation(
                match(where("userId").is(userId)
                        .and("date").gte(month.atDay(1)).lt(month.plusMonths(1).atDay(1))),
                group("category").sum("amount").as("spent").count().as("count"));

        Map<Category, CategoryTotal> totals = new EnumMap<>(Category.class);
        for (Document row : mongoTemplate.aggregate(aggregation, Expense.class, Document.class)) {
            Category category = Category.valueOf(row.getString("_id"));
            totals.put(category, new CategoryTotal(toBigDecimal(row.get("spent")),
                    ((Number) row.get("count")).longValue()));
        }

        Map<Category, BigDecimal> limits = new EnumMap<>(Category.class);
        for (Budget budget : budgets.findByUserIdOrderByCategory(userId)) {
            limits.put(budget.getCategory(), budget.getMonthlyLimit());
        }

        return calculator.build(month, totals, limits);
    }

    private static BigDecimal toBigDecimal(Object value) {
        if (value instanceof Decimal128 d) return d.bigDecimalValue();
        if (value instanceof BigDecimal b) return b;
        if (value instanceof Number n) return new BigDecimal(n.toString());
        return BigDecimal.ZERO;
    }
}
