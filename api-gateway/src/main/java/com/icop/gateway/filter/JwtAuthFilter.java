package com.icop.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * The gateway's front-door auth check. Every request into the platform passes
 * through here first: validate the JWT once at the edge, then forward the
 * caller's identity downstream as plain headers.
 *
 * The payoff of doing it here: the individual services can trust
 * X-User-Email / X-User-Role and skip re-parsing the token themselves. This
 * is a reactive GlobalFilter (Spring Cloud Gateway runs on WebFlux), so
 * everything returns a Mono rather than blocking.
 */
@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    // the only routes reachable without a token — you can't present a JWT
    // before you've logged in to get one, and actuator is for the probes
    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/auth/register",
            "/api/auth/login",
            "/actuator"
    );

    @Value("${jwt.secret}")
    private String secret;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // short-circuit with a 401 — setComplete() ends the exchange here
            // without ever touching a downstream service
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        try {
            String token = authHeader.substring(7);
            Claims claims = extractClaims(token); // throws if signature/expiry is bad

            // stamp identity onto the forwarded request. downstream services
            // read these instead of re-validating the token
            ServerWebExchange mutatedExchange = exchange.mutate()
                    .request(r -> r.header("X-User-Email", claims.getSubject())
                            .header("X-User-Role", claims.get("role", String.class)))
                    .build();

            return chain.filter(mutatedExchange);
        } catch (Exception e) {
            // any token problem is a 401 — don't leak which check failed
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    private Claims extractClaims(String token) {
        // same HMAC secret the user-service signs with — shared via config/secret
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // -1 so this runs before the routing filters — auth has to happen before
    // the request gets proxied anywhere
    @Override
    public int getOrder() {
        return -1;
    }
}
