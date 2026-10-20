package com.saicharan.expensetracker.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank @Size(max = 80) String fullName,
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, max = 72, message = "must be 8 to 72 characters") String password) {
    }

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {
    }

    public record AuthResponse(String token, String userId, String fullName, String email) {
    }
}
