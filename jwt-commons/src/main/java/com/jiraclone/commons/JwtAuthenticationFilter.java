// jwt-commons/src/main/java/com/jiraclone/commons/JwtAuthenticationFilter.java
package com.jiraclone.commons;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Set;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Set<String> PUBLIC_AUTH_PATHS = Set.of(
            "/auth/login",
            "/auth/register",
            "/auth/refresh",
            "/auth/logout"
    );

    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return PUBLIC_AUTH_PATHS.contains(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        SecurityContextHolder.clearContext();

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (jwtTokenProvider.validateToken(token) && jwtTokenProvider.isAccessToken(token)) {
                setAuthentication(request, token);
            }
        }

        filterChain.doFilter(request, response);
    }

    private void setAuthentication(HttpServletRequest request, String token) {
        String userId = jwtTokenProvider.getUserIdFromToken(token);
        List<SimpleGrantedAuthority> authorities = jwtTokenProvider.getRolesFromToken(token).stream()
                .map(SimpleGrantedAuthority::new)
                .toList();

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userId, null, authorities);
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
