package com.saicharan.expensetracker.budget;

import com.saicharan.expensetracker.expense.Category;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;

/** Monthly spending limit for one category. One budget per user per category. */
@Document("budgets")
@CompoundIndex(name = "user_category_uq", def = "{'userId': 1, 'category': 1}", unique = true)
public class Budget {

    @Id
    private String id;

    private String userId;
    private Category category;

    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal monthlyLimit;

    protected Budget() {
    }

    public Budget(String userId, Category category, BigDecimal monthlyLimit) {
        this.userId = userId;
        this.category = category;
        this.monthlyLimit = monthlyLimit;
    }

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public Category getCategory() { return category; }
    public BigDecimal getMonthlyLimit() { return monthlyLimit; }

    public void setMonthlyLimit(BigDecimal monthlyLimit) { this.monthlyLimit = monthlyLimit; }
}
