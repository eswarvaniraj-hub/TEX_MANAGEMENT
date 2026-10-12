package com.textile.accounts.auth.dto;

public record UserProfileResponse(
        String username,
        String role,
        boolean mustChangePassword
) {}
