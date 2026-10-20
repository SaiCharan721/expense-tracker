package com.saicharan.expensetracker.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET = "ZGV2LW9ubHktc2VjcmV0LWNoYW5nZS1tZS1pbi1wcm9kdWN0aW9uLTEyMzQ1Njc4OTA=";

    @Test
    void tokenRoundTripKeepsUserIdAndEmail() {
        JwtService jwt = new JwtService(SECRET, 60);
        String token = jwt.generateToken("u-123", "sai@example.com");

        assertThat(jwt.parse(token)).contains(new AuthUser("u-123", "sai@example.com"));
    }

    @Test
    void rejectsTamperedToken() {
        JwtService jwt = new JwtService(SECRET, 60);
        String token = jwt.generateToken("u-123", "sai@example.com");
        String tampered = token.substring(0, token.length() - 4) + "abcd";

        assertThat(jwt.parse(tampered)).isEmpty();
        assertThat(jwt.parse("not-a-jwt")).isEmpty();
    }

    @Test
    void rejectsExpiredToken() {
        JwtService alreadyExpired = new JwtService(SECRET, -1);
        String token = alreadyExpired.generateToken("u-123", "sai@example.com");

        assertThat(new JwtService(SECRET, 60).parse(token)).isEmpty();
    }
}
