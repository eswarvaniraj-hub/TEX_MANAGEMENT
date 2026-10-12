package com.textile.accounts.auth.dto;

public record CsrfResponse(
        String token,
        String headerName,
        String parameterName
) {}
