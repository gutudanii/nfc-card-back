package com.toollix.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

@Component
@Order(1)
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;
    private final int authLimit;
    private final int tapLimit;

    public RateLimitFilter(RateLimitService rateLimitService,
                          @Value("${security.rate-limit.auth-max:10}") int authLimit,
                          @Value("${security.rate-limit.tap-max:30}") int tapLimit) {
        this.rateLimitService = rateLimitService;
        this.authLimit = authLimit;
        this.tapLimit = tapLimit;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith("/auth") && !path.startsWith("/tap");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        String key = path.startsWith("/auth") ? "auth:" + request.getRemoteAddr() : "tap:" + request.getRemoteAddr();
        int max = path.startsWith("/auth") ? authLimit : tapLimit;
        if (!rateLimitService.allow(key, max, Duration.ofMinutes(1))) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"rate_limit_exceeded\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
