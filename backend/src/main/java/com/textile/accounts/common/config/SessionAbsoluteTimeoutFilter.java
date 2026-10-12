package com.textile.accounts.common.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class SessionAbsoluteTimeoutFilter extends OncePerRequestFilter {

    // 12 hours in milliseconds = 12 * 60 * 60 * 1000 = 43,200,000 ms
    public static final long MAX_SESSION_DURATION_MS = 12L * 60L * 60L * 1000L;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session != null) {
            long creationTime = session.getCreationTime();
            long now = System.currentTimeMillis();

            if (now - creationTime > MAX_SESSION_DURATION_MS) {
                session.invalidate();
                SecurityContextHolder.clearContext();

                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"SESSION_EXPIRED\", \"message\": \"Session has exceeded the maximum limit of 12 hours\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
