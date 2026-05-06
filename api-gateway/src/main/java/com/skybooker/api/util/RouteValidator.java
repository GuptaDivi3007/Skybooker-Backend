package com.skybooker.api.util;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.List;

@Component
public class RouteValidator {

    private final List<String> openApiEndpoints = List.of(
            "/auth/register",
            "/auth/login",
            "/auth/refresh",
            "/auth/validate",
            "/oauth2/**",
            "/login/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/actuator/**"
    );

    private final List<String> publicFlightEndpoints = List.of(
            "/flights/search/**",
            "/flights/public/**"
    );

    private final List<String> publicBookingEndpoints = List.of(
            "/bookings/pnr/**"
    );

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public boolean isSecured(ServerHttpRequest request) {
        String path = request.getURI().getPath();

        for (String pattern : openApiEndpoints) {
            if (pathMatcher.match(pattern, path)) {
                return false;
            }
        }

        for (String pattern : publicFlightEndpoints) {
            if (pathMatcher.match(pattern, path)) {
                return false;
            }
        }

        for (String pattern : publicBookingEndpoints) {
            if (pathMatcher.match(pattern, path)) {
                return false;
            }
        }

        return true;
    }

    public boolean isAdminPath(ServerHttpRequest request) {
        return request.getURI().getPath().startsWith("/admin/");
    }
}