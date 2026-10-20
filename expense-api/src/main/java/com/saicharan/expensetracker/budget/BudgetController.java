package com.saicharan.expensetracker.budget;

import com.saicharan.expensetracker.budget.BudgetService.BudgetResponse;
import com.saicharan.expensetracker.expense.Category;
import com.saicharan.expensetracker.security.AuthUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/budgets")
public class BudgetController {

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @GetMapping
    public List<BudgetResponse> list(@AuthenticationPrincipal AuthUser user) {
        return budgetService.list(user.id());
    }

    @PutMapping("/{category}")
    public BudgetResponse set(@AuthenticationPrincipal AuthUser user, @PathVariable Category category,
                              @Valid @RequestBody BudgetRequest request) {
        return budgetService.upsert(user.id(), category, request.monthlyLimit());
    }

    @DeleteMapping("/{category}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthUser user, @PathVariable Category category) {
        budgetService.delete(user.id(), category);
    }

    public record BudgetRequest(
            @NotNull @DecimalMin(value = "1.00", message = "must be at least 1.00")
            @Digits(integer = 9, fraction = 2) BigDecimal monthlyLimit) {
    }
}
