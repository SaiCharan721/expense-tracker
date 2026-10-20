package com.saicharan.expensetracker.expense;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class ExpenseDtos {

    private ExpenseDtos() {
    }

    public record ExpenseRequest(
            @NotNull @DecimalMin(value = "0.01", message = "must be at least 0.01")
            @Digits(integer = 9, fraction = 2) BigDecimal amount,
            @NotNull Category category,
            @NotNull @PastOrPresent LocalDate date,
            @Size(max = 200) String description,
            PaymentMethod paymentMethod,
            @Size(max = 5, message = "at most 5 tags") List<@Size(max = 20) String> tags) {
    }

    public record ExpenseResponse(
            String id,
            BigDecimal amount,
            Category category,
            LocalDate date,
            String description,
            PaymentMethod paymentMethod,
            List<String> tags) {

        static ExpenseResponse from(Expense e) {
            return new ExpenseResponse(e.getId(), e.getAmount(), e.getCategory(), e.getDate(),
                    e.getDescription(), e.getPaymentMethod(), e.getTags());
        }
    }

    /** All fields optional. Dates are inclusive. */
    public record ExpenseFilter(LocalDate from, LocalDate to, Category category, String q) {
    }
}
