package com.saicharan.expensetracker.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Runs the real API against MongoDB in a container (needs Docker). */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ExpenseApiIntegrationTest {

    @Container
    @ServiceConnection
    static MongoDBContainer mongo = new MongoDBContainer("mongo:7.0");

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    // keep test dates inside one month and never in the future (@PastOrPresent)
    private final YearMonth lastMonth = YearMonth.now().minusMonths(1);

    @Test
    void createUpdateAndDeleteAnExpense() throws Exception {
        String token = registerNewUser();

        String id = createExpense(token, "42.75", "FOOD", lastMonth.atDay(3), "Lunch with team");

        mvc.perform(auth(get("/api/expenses/" + id), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(42.75))
                .andExpect(jsonPath("$.category").value("FOOD"))
                .andExpect(jsonPath("$.paymentMethod").value("OTHER"));

        mvc.perform(auth(put("/api/expenses/" + id), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(expenseJson("39.99", "FOOD", lastMonth.atDay(3), "Lunch (split bill)")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(39.99))
                .andExpect(jsonPath("$.description").value("Lunch (split bill)"));

        mvc.perform(auth(delete("/api/expenses/" + id), token)).andExpect(status().isNoContent());
        mvc.perform(auth(get("/api/expenses/" + id), token)).andExpect(status().isNotFound());
    }

    @Test
    void filtersByDateRangeCategoryAndText() throws Exception {
        String token = registerNewUser();
        createExpense(token, "12.00", "FOOD", lastMonth.atDay(2), "Chipotle");
        createExpense(token, "55.00", "TRANSPORT", lastMonth.atDay(5), "Gas");
        createExpense(token, "18.40", "FOOD", lastMonth.atDay(20), "Chipotle again");
        createExpense(token, "9.00", "FOOD", lastMonth.minusMonths(1).atDay(15), "Old coffee");

        mvc.perform(auth(get("/api/expenses"), token)
                        .param("from", lastMonth.atDay(1).toString())
                        .param("to", lastMonth.atEndOfMonth().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                // newest first
                .andExpect(jsonPath("$.content[0].description").value("Chipotle again"));

        mvc.perform(auth(get("/api/expenses"), token).param("category", "FOOD").param("q", "chipotle"))
                .andExpect(jsonPath("$.totalElements").value(2));

        mvc.perform(auth(get("/api/expenses"), token).param("size", "2").param("page", "1"))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void usersCannotSeeEachOthersExpenses() throws Exception {
        String alice = registerNewUser();
        String bob = registerNewUser();
        String aliceExpense = createExpense(alice, "100.00", "SHOPPING", lastMonth.atDay(1), "Shoes");

        mvc.perform(auth(get("/api/expenses/" + aliceExpense), bob)).andExpect(status().isNotFound());
        mvc.perform(auth(delete("/api/expenses/" + aliceExpense), bob)).andExpect(status().isNotFound());
        mvc.perform(auth(get("/api/expenses"), bob)).andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void monthlySummaryComparesSpendingWithBudgets() throws Exception {
        String token = registerNewUser();
        createExpense(token, "120.50", "FOOD", lastMonth.atDay(4), "Groceries for party");
        createExpense(token, "50.00", "FOOD", lastMonth.atDay(18), "Dinner");
        createExpense(token, "1000.00", "RENT", lastMonth.atDay(1), "Rent");
        createExpense(token, "75.00", "FOOD", lastMonth.minusMonths(1).atDay(28), "Previous month");

        mvc.perform(auth(put("/api/budgets/FOOD"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monthlyLimit\": 200}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monthlyLimit").value(200.00));

        mvc.perform(auth(get("/api/summary"), token).param("month", lastMonth.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSpent").value(1170.50))
                .andExpect(jsonPath("$.expenseCount").value(3))
                .andExpect(jsonPath("$.categories[0].category").value("RENT"))
                .andExpect(jsonPath("$.categories[0].status").value("NO_BUDGET"))
                .andExpect(jsonPath("$.categories[1].category").value("FOOD"))
                .andExpect(jsonPath("$.categories[1].spent").value(170.50))
                .andExpect(jsonPath("$.categories[1].percentUsed").value(85))
                .andExpect(jsonPath("$.categories[1].status").value("WARNING"));
    }

    @Test
    void rejectsInvalidInput() throws Exception {
        String token = registerNewUser();

        mvc.perform(auth(post("/api/expenses"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(expenseJson("-5", "FOOD", LocalDate.now().plusDays(3), "bad")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.amount").exists())
                .andExpect(jsonPath("$.errors.date").exists());

        mvc.perform(auth(post("/api/expenses"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(expenseJson("5", "PIZZA", lastMonth.atDay(1), "unknown category")))
                .andExpect(status().isBadRequest());

        mvc.perform(auth(get("/api/summary"), token).param("month", "October"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void authRules() throws Exception {
        mvc.perform(get("/api/expenses")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/expenses").header("Authorization", "Bearer garbage"))
                .andExpect(status().isUnauthorized());

        String email = "dup-" + UUID.randomUUID() + "@example.com";
        register(email).andExpect(status().isCreated());
        register(email.toUpperCase()).andExpect(status().isConflict());

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", email, "password", "wrong-password"))))
                .andExpect(status().isUnauthorized());
    }

    // ---- helpers ----

    private String registerNewUser() throws Exception {
        String body = register("user-" + UUID.randomUUID() + "@example.com")
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("token").asText();
    }

    private org.springframework.test.web.servlet.ResultActions register(String email) throws Exception {
        return mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of(
                        "fullName", "Test User", "email", email, "password", "password123"))));
    }

    private String createExpense(String token, String amount, String category, LocalDate date, String description)
            throws Exception {
        String body = mvc.perform(auth(post("/api/expenses"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(expenseJson(amount, category, date, description)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode node = json.readTree(body);
        return node.get("id").asText();
    }

    private String expenseJson(String amount, String category, LocalDate date, String description) {
        return """
                {"amount": %s, "category": "%s", "date": "%s", "description": "%s"}
                """.formatted(amount, category, date, description);
    }

    private static MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder request, String token) {
        return request.header("Authorization", "Bearer " + token);
    }
}
