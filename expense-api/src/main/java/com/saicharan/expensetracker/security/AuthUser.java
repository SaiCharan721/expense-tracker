package com.saicharan.expensetracker.security;

/** What the JWT filter puts in the security context. Controllers get it with @AuthenticationPrincipal. */
public record AuthUser(String id, String email) {
}
