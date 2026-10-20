package com.saicharan.expensetracker.config;

import com.saicharan.expensetracker.budget.Budget;
import com.saicharan.expensetracker.budget.BudgetRepository;
import com.saicharan.expensetracker.expense.Category;
import com.saicharan.expensetracker.expense.Expense;
import com.saicharan.expensetracker.expense.ExpenseRepository;
import com.saicharan.expensetracker.expense.PaymentMethod;
import com.saicharan.expensetracker.user.User;
import com.saicharan.expensetracker.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Loads a demo account with about three months of expenses so the dashboard
 * has something to show. Only runs with the "demo" profile (docker-compose uses it).
 */
@Component
@Profile("demo")
public class DemoDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);
    static final String DEMO_EMAIL = "demo@expensetracker.dev";

    private record Template(Category category, String description, double min, double max, PaymentMethod method) {
    }

    private static final List<Template> TEMPLATES = List.of(
            new Template(Category.GROCERIES, "Kroger", 25, 140, PaymentMethod.CARD),
            new Template(Category.GROCERIES, "Costco run", 60, 220, PaymentMethod.CARD),
            new Template(Category.FOOD, "Chipotle", 11, 18, PaymentMethod.CARD),
            new Template(Category.FOOD, "Coffee", 4, 7, PaymentMethod.CARD),
            new Template(Category.FOOD, "Dinner with friends", 30, 75, PaymentMethod.CARD),
            new Template(Category.TRANSPORT, "Gas", 35, 55, PaymentMethod.CARD),
            new Template(Category.TRANSPORT, "Uber", 12, 30, PaymentMethod.CARD),
            new Template(Category.SHOPPING, "Amazon order", 15, 90, PaymentMethod.CARD),
            new Template(Category.ENTERTAINMENT, "Movie tickets", 14, 32, PaymentMethod.CARD),
            new Template(Category.HEALTH, "Pharmacy", 8, 40, PaymentMethod.CASH));

    private final UserRepository users;
    private final ExpenseRepository expenses;
    private final BudgetRepository budgets;
    private final PasswordEncoder passwordEncoder;

    public DemoDataSeeder(UserRepository users, ExpenseRepository expenses, BudgetRepository budgets,
                          PasswordEncoder passwordEncoder) {
        this.users = users;
        this.expenses = expenses;
        this.budgets = budgets;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (users.existsByEmail(DEMO_EMAIL)) {
            return;
        }
        User demo = users.save(new User("Demo User", DEMO_EMAIL, passwordEncoder.encode("demo1234")));
        String userId = demo.getId();
        Random random = new Random(42);
        LocalDate today = LocalDate.now();
        List<Expense> batch = new ArrayList<>();

        for (int monthsBack = 2; monthsBack >= 0; monthsBack--) {
            LocalDate first = today.minusMonths(monthsBack).withDayOfMonth(1);
            LocalDate last = monthsBack == 0 ? today : first.plusMonths(1).minusDays(1);

            batch.add(expense(userId, Category.RENT, "Apartment rent", new BigDecimal("1350.00"), first, PaymentMethod.BANK_TRANSFER));
            if (!first.plusDays(14).isAfter(last)) {
                batch.add(expense(userId, Category.UTILITIES, "Electric bill", money(random, 70, 120), first.plusDays(14), PaymentMethod.BANK_TRANSFER));
                batch.add(expense(userId, Category.UTILITIES, "Internet", new BigDecimal("65.00"), first.plusDays(14), PaymentMethod.CARD));
            }

            int days = last.getDayOfMonth();
            for (int i = 0; i < days; i++) {
                if (random.nextInt(10) < 4) continue; // not every day has a purchase
                Template t = TEMPLATES.get(random.nextInt(TEMPLATES.size()));
                batch.add(expense(userId, t.category(), t.description(), money(random, t.min(), t.max()),
                        first.plusDays(i), t.method()));
            }
        }
        expenses.saveAll(batch);

        budgets.saveAll(List.of(
                new Budget(userId, Category.GROCERIES, new BigDecimal("450.00")),
                new Budget(userId, Category.FOOD, new BigDecimal("250.00")),
                new Budget(userId, Category.TRANSPORT, new BigDecimal("200.00")),
                new Budget(userId, Category.SHOPPING, new BigDecimal("150.00")),
                new Budget(userId, Category.RENT, new BigDecimal("1350.00"))));

        log.info("Seeded demo account {} with {} expenses", DEMO_EMAIL, batch.size());
    }

    private static Expense expense(String userId, Category category, String description, BigDecimal amount,
                                   LocalDate date, PaymentMethod method) {
        Expense e = new Expense(userId);
        e.setCategory(category);
        e.setDescription(description);
        e.setAmount(amount);
        e.setDate(date);
        e.setPaymentMethod(method);
        return e;
    }

    private static BigDecimal money(Random random, double min, double max) {
        return BigDecimal.valueOf(min + random.nextDouble() * (max - min)).setScale(2, RoundingMode.HALF_UP);
    }
}
