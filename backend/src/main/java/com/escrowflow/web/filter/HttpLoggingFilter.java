package com.escrowflow.web.filter;

import com.escrowflow.security.SecurityUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class HttpLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        if (!request.getRequestURI().startsWith("/api")) {
            filterChain.doFilter(request, response);
            return;
        }

        long startMs = System.currentTimeMillis();
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String query = request.getQueryString();
        String path = query == null ? uri : uri + "?" + query;

        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = System.currentTimeMillis() - startMs;
            int status = response.getStatus();
            String user = resolveUserLabel();

            if (status >= 500) {
                log.error("API {} {} -> {} ({}ms) user={}", method, path, status, durationMs, user);
            } else if (status >= 400) {
                log.warn("API {} {} -> {} ({}ms) user={}", method, path, status, durationMs, user);
            } else if ("GET".equals(method)) {
                log.debug("API {} {} -> {} ({}ms) user={}", method, path, status, durationMs, user);
            } else {
                log.info("API {} {} -> {} ({}ms) user={}", method, path, status, durationMs, user);
            }
        }
    }

    private String resolveUserLabel() {
        try {
            var principal = SecurityUtils.getCurrentUser();
            return principal.getUserId() + ":" + principal.getEmail();
        } catch (Exception ex) {
            return "anonymous";
        }
    }
}
