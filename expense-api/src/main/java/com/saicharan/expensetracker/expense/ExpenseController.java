package com.saicharan.expensetracker.expense;

import com.saicharan.expensetracker.common.PageResponse;
import com.saicharan.expensetracker.expense.ExpenseDtos.ExpenseFilter;
import com.saicharan.expensetracker.expense.ExpenseDtos.ExpenseRequest;
import com.saicharan.expensetracker.expense.ExpenseDtos.ExpenseResponse;
import com.saicharan.expensetracker.security.AuthUser;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    private static final int MAX_PAGE_SIZE = 100;

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @GetMapping
    public PageResponse<ExpenseResponse> list(
            @AuthenticationPrincipal AuthUser user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Category category,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
        return expenseService.search(user.id(), new ExpenseFilter(from, to, category, q), pageable);
    }

    @GetMapping("/{id}")
    public ExpenseResponse get(@AuthenticationPrincipal AuthUser user, @PathVariable String id) {
        return expenseService.get(user.id(), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExpenseResponse create(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody ExpenseRequest request) {
        return expenseService.create(user.id(), request);
    }

    @PutMapping("/{id}")
    public ExpenseResponse update(@AuthenticationPrincipal AuthUser user, @PathVariable String id,
                                  @Valid @RequestBody ExpenseRequest request) {
        return expenseService.update(user.id(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthUser user, @PathVariable String id) {
        expenseService.delete(user.id(), id);
    }
}
