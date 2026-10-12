package com.textile.accounts.auth.controller;

import com.textile.accounts.auth.dto.ChangePasswordRequest;
import com.textile.accounts.auth.dto.CsrfResponse;
import com.textile.accounts.auth.dto.LoginRequest;
import com.textile.accounts.auth.dto.UserProfileResponse;
import com.textile.accounts.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<UserProfileResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        UserProfileResponse response = authService.login(request, httpRequest, httpResponse);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getCurrentUser(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        UserProfileResponse response = authService.getCurrentUserProfile(principal.getName());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/change-password")
    public ResponseEntity<UserProfileResponse> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Principal principal,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        UserProfileResponse response = authService.changePassword(request, principal.getName(), httpRequest, httpResponse);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            Principal principal,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        String username = principal != null ? principal.getName() : null;
        authService.logout(username, httpRequest, httpResponse);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/csrf")
    public ResponseEntity<CsrfResponse> getCsrfToken(HttpServletRequest request) {
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(new CsrfResponse(
                csrfToken.getToken(),
                csrfToken.getHeaderName(),
                csrfToken.getParameterName()
        ));
    }
}
