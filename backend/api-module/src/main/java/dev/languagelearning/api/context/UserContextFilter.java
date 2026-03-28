package dev.languagelearning.api.context;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Servlet filter that extracts the X-User-Id header from incoming requests
 * and populates the request-scoped {@link UserContext}.
 * <p>
 * This enables multi-user support where the frontend specifies which user
 * is making the request via the X-User-Id header.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class UserContextFilter extends OncePerRequestFilter {

    public static final String USER_ID_HEADER = "X-User-Id";

    private final UserContext userContext;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String userIdHeader = request.getHeader(USER_ID_HEADER);

        if (userIdHeader != null && !userIdHeader.isBlank()) {
            try {
                UUID userId = UUID.fromString(userIdHeader.trim());
                userContext.setUserId(userId);
                log.debug("Set user context from header: {}", userId);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid X-User-Id header value: {}", userIdHeader);
                // Continue without setting user context - service will handle missing context
            }
        }

        filterChain.doFilter(request, response);
    }
}