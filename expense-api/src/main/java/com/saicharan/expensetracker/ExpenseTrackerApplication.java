package com.saicharan.expensetracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

// Users are loaded from MongoDB in AuthService, so the in-memory default user is not needed.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class ExpenseTrackerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExpenseTrackerApplication.class, args);
    }
}
