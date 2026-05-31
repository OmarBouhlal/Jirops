// planning-service/src/main/java/com/jiraclone/planning/observability/UserSpanTagFilter.java
package com.jiraclone.planning.observability;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class UserSpanTagFilter extends OncePerRequestFilter {

    private final Tracer tracer;

    public UserSpanTagFilter(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        tagCurrentSpan(request);
        filterChain.doFilter(request, response);
    }

    private void tagCurrentSpan(HttpServletRequest request) {
        String userId = request.getHeader("X-User-Id");
        if (userId == null || userId.isBlank()) {
            return;
        }

        Span span = tracer.currentSpan();
        if (span != null) {
            span.tag("X-User-Id", userId.trim());
        }
    }
}
