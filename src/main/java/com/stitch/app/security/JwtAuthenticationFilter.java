package com.stitch.app.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        // Log ALL requests to messages endpoints for debugging
        if (request.getRequestURI().startsWith("/api/messages")) {
            logger.info("=== JWT Filter processing request: {} {} ===", request.getMethod(), request.getRequestURI());
            logger.info("Authorization header present: {}", authHeader != null);
            if (authHeader != null) {
                logger.info("Authorization header value: {}", authHeader.substring(0, Math.min(30, authHeader.length())) + "...");
            }
        }

        // Helpful debug logging to diagnose missing Authorization header issues
        if (authHeader == null) {
            logger.debug("No Authorization header present for request: {} {}", request.getMethod(), request.getRequestURI());

            // Fallback: allow token via access_token query parameter (useful for clients that can't set headers)
            String accessTokenParam = request.getParameter("access_token");
            if (accessTokenParam != null && !accessTokenParam.isBlank()) {
                logger.debug("Found access_token query parameter for request {} {} - using it as Bearer token", request.getMethod(), request.getRequestURI());
            }
        } else if (!authHeader.startsWith("Bearer ")) {
            logger.debug("Authorization header does not start with 'Bearer ' for request: {} {} - header='{}'", request.getMethod(), request.getRequestURI(), authHeader);
        }

        // Check if Authorization header exists and starts with "Bearer ", otherwise try access_token param or cookie
        String tokenCandidate = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            tokenCandidate = authHeader.substring(7);
        } else {
            String accessTokenParam = request.getParameter("access_token");
            if (accessTokenParam != null && !accessTokenParam.isBlank()) {
                tokenCandidate = accessTokenParam;
            } else {
                // Check cookies for token fallback
                if (request.getCookies() != null) {
                    for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                        if ("jwt".equalsIgnoreCase(cookie.getName()) || "access_token".equalsIgnoreCase(cookie.getName())) {
                            if (cookie.getValue() != null && !cookie.getValue().isBlank()) {
                                tokenCandidate = cookie.getValue();
                                logger.debug("Found JWT in cookie '{}' for request {} {}", cookie.getName(), request.getMethod(), request.getRequestURI());
                                break;
                            }
                        }
                    }
                }
            }
        }

        // If no token found, continue filter chain unauthenticated
        if (tokenCandidate == null) {
            // Log for ALL /api/messages requests missing token
            String reqUri = request.getRequestURI();
            if (reqUri.startsWith("/api/messages")) {
                logger.error("Request to {} {} has no JWT token attached (no Authorization header, access_token param, or jwt cookie). Will return 401.",
                        request.getMethod(), reqUri);
            }
            filterChain.doFilter(request, response);
            return;
        }

        // Extract JWT token
        jwt = tokenCandidate;

        try {
            // Extract user email from JWT
            userEmail = jwtService.extractUsername(jwt);

            // If user email exists and user is not already authenticated
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

                // Validate token
                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    // Log debug info about authenticated user
                    String authorities = userDetails.getAuthorities().stream()
                            .map(GrantedAuthority::getAuthority)
                            .collect(Collectors.joining(","));
                    logger.info("JWT authentication successful for user='{}' authorities='{}' request='{} {}'",
                            userEmail, authorities, request.getMethod(), request.getRequestURI());
                } else {
                    logger.error("JWT token is NOT VALID for user: {} for request {} {} - Token may be expired or invalid",
                            userEmail, request.getMethod(), request.getRequestURI());
                }
            }
        } catch (Exception e) {
            // Log the exception if needed
            logger.error("Cannot set user authentication: {}", e.toString());
        }

        filterChain.doFilter(request, response);
    }
}