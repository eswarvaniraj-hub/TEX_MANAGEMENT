package com.textile.accounts.common.config;

import com.textile.accounts.auth.model.AppUser;
import com.textile.accounts.auth.repository.AppUserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

public class MustChangePasswordFilter extends OncePerRequestFilter {

    private static final Set<String> ALLOWED_PATHS = Set.of(
            "/api/v1/auth/me",
            "/api/v1/auth/logout",
            "/api/v1/auth/change-password",
            "/api/v1/auth/csrf",
            "/api/v1/health",
            "/actuator/health"
    );

    private final AppUserRepository appUserRepository;

    public MustChangePasswordFilter(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();
        if (isAllowedPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            String username = auth.getName();
            Optional<AppUser> userOpt = appUserRepository.findByUsernameIgnoreCase(username);

            if (userOpt.isPresent() && userOpt.get().isMustChangePassword()) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                response.getWriter().write(
                    "{\"error\": \"PASSWORD_CHANGE_REQUIRED\", \"message\": \"Password change is required before accessing other resources\"}"
                );
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isAllowedPath(String path) {
        if (path == null) return false;
        for (String allowed : ALLOWED_PATHS) {
            if (path.equals(allowed) || path.endsWith(allowed)) {
                return true;
            }
        }
        return false;
    }
}
