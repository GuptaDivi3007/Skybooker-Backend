package com.skybooker.api.filter;

import com.skybooker.api.dto.TokenValidationResponse;
import com.skybooker.api.util.RouteValidator;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    private final RouteValidator routeValidator;
    private final WebClient.Builder webClientBuilder;

    public AuthenticationFilter(RouteValidator routeValidator, WebClient.Builder webClientBuilder) {
        super(Config.class);
        this.routeValidator = routeValidator;
        this.webClientBuilder = webClientBuilder;
    }

    public static class Config {
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {

            if (!routeValidator.isSecured(exchange.getRequest())) {
                return chain.filter(exchange);
            }

            String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            if (authHeader == null || authHeader.isBlank()) {
                return onError(exchange, HttpStatus.UNAUTHORIZED, "Missing Authorization header");
            }

            if (!authHeader.startsWith("Bearer ")) {
                return onError(exchange, HttpStatus.UNAUTHORIZED, "Invalid Authorization header");
            }

            String token = authHeader.substring(7);

            return webClientBuilder.build()
                    .get()
                    .uri("lb://auth-service/auth/validate?token={token}", token)
                    .retrieve()
                    .bodyToMono(TokenValidationResponse.class)
                    .flatMap((TokenValidationResponse validation) -> {
                        if (validation == null || !validation.valid()) {
                            return onError(exchange, HttpStatus.UNAUTHORIZED, "Invalid token");
                        }

                        if (!validation.active()) {
                            return onError(exchange, HttpStatus.UNAUTHORIZED, "User account is inactive");
                        }

                        if (routeValidator.isAdminPath(exchange.getRequest())
                                && !"ADMIN".equals(validation.role())) {
                            return onError(exchange, HttpStatus.FORBIDDEN, "Admin access required");
                        }

                        ServerHttpRequest mutatedRequest = exchange.getRequest()
                                .mutate()
                                .header("X-Authenticated-User-Email", validation.email() == null ? "" : validation.email())
                                .header("X-Authenticated-User-Id", validation.userId() == null ? "" : validation.userId())
                                .header("X-Authenticated-User-Role", validation.role() == null ? "" : validation.role())
                                .build();

                        return chain.filter(exchange.mutate().request(mutatedRequest).build());
                    })
                    .onErrorResume(ex -> onError(exchange, HttpStatus.UNAUTHORIZED, "Token validation failed"));
        };
    }

    private Mono<Void> onError(ServerWebExchange exchange, HttpStatusCode status, String message) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String body = """
                {
                  "status": %d,
                  "error": "%s",
                  "message": "%s"
                }
                """.formatted(status.value(), "Request Failed", message);

        DataBuffer buffer = exchange.getResponse()
                .bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));

        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}