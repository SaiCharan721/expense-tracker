package com.saicharan.expensetracker.expense;

import com.saicharan.expensetracker.common.NotFoundException;
import com.saicharan.expensetracker.common.PageResponse;
import com.saicharan.expensetracker.expense.ExpenseDtos.ExpenseFilter;
import com.saicharan.expensetracker.expense.ExpenseDtos.ExpenseRequest;
import com.saicharan.expensetracker.expense.ExpenseDtos.ExpenseResponse;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class ExpenseService {

    private final ExpenseRepository expenses;
    private final MongoTemplate mongoTemplate;

    public ExpenseService(ExpenseRepository expenses, MongoTemplate mongoTemplate) {
        this.expenses = expenses;
        this.mongoTemplate = mongoTemplate;
    }

    public ExpenseResponse create(String userId, ExpenseRequest req) {
        Expense expense = new Expense(userId);
        apply(expense, req);
        return ExpenseResponse.from(expenses.save(expense));
    }

    public ExpenseResponse get(String userId, String id) {
        return ExpenseResponse.from(load(userId, id));
    }

    public ExpenseResponse update(String userId, String id, ExpenseRequest req) {
        Expense expense = load(userId, id);
        apply(expense, req);
        expense.setUpdatedAt(Instant.now());
        return ExpenseResponse.from(expenses.save(expense));
    }

    public void delete(String userId, String id) {
        if (expenses.deleteByIdAndUserId(id, userId) == 0) {
            throw notFound(id);
        }
    }

    public PageResponse<ExpenseResponse> search(String userId, ExpenseFilter filter, Pageable pageable) {
        Criteria criteria = Criteria.where("userId").is(userId);
        if (filter.from() != null || filter.to() != null) {
            Criteria date = criteria.and("date");
            if (filter.from() != null) date.gte(filter.from());
            if (filter.to() != null) date.lte(filter.to());
        }
        if (filter.category() != null) {
            criteria.and("category").is(filter.category());
        }
        if (StringUtils.hasText(filter.q())) {
            // quote the input so characters like "(" or "*" are searched literally
            Pattern pattern = Pattern.compile(Pattern.quote(filter.q().trim()), Pattern.CASE_INSENSITIVE);
            criteria.and("description").regex(pattern);
        }

        Query query = new Query(criteria);
        long total = mongoTemplate.count(query, Expense.class);

        query.with(pageable).with(Sort.by(Sort.Direction.DESC, "date", "createdAt"));
        List<ExpenseResponse> content = mongoTemplate.find(query, Expense.class).stream()
                .map(ExpenseResponse::from)
                .toList();
        return PageResponse.of(new PageImpl<>(content, pageable, total));
    }

    private Expense load(String userId, String id) {
        // filtering by userId as well means another user's id just looks like a missing record
        return expenses.findByIdAndUserId(id, userId).orElseThrow(() -> notFound(id));
    }

    private static void apply(Expense expense, ExpenseRequest req) {
        expense.setAmount(req.amount().setScale(2, RoundingMode.HALF_UP));
        expense.setCategory(req.category());
        expense.setDate(req.date());
        expense.setDescription(req.description() == null ? null : req.description().trim());
        expense.setPaymentMethod(req.paymentMethod() == null ? PaymentMethod.OTHER : req.paymentMethod());
        expense.setTags(req.tags() == null ? List.of() : req.tags().stream()
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(String::toLowerCase)
                .distinct()
                .toList());
    }

    private static NotFoundException notFound(String id) {
        return new NotFoundException("Expense " + id + " not found");
    }
}
