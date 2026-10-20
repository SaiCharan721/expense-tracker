package com.saicharan.expensetracker.expense;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Document("expenses")
@CompoundIndex(name = "user_date_idx", def = "{'userId': 1, 'date': -1}")
public class Expense {

    @Id
    private String id;

    private String userId;

    // Decimal128 instead of the default String mapping, so $sum works in aggregations
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal amount;

    private Category category;
    private LocalDate date;
    private String description;
    private PaymentMethod paymentMethod;
    private List<String> tags = new ArrayList<>();
    private Instant createdAt;
    private Instant updatedAt;

    protected Expense() {
    }

    public Expense(String userId) {
        this.userId = userId;
        this.createdAt = Instant.now();
    }

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public BigDecimal getAmount() { return amount; }
    public Category getCategory() { return category; }
    public LocalDate getDate() { return date; }
    public String getDescription() { return description; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public List<String> getTags() { return tags; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public void setCategory(Category category) { this.category = category; }
    public void setDate(LocalDate date) { this.date = date; }
    public void setDescription(String description) { this.description = description; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
