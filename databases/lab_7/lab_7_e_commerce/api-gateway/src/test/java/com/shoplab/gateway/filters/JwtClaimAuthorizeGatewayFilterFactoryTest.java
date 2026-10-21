package com.shoplab.gateway.filters;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;

class JwtClaimAuthorizeGatewayFilterFactoryTest {

    private final JwtClaimAuthorizeGatewayFilterFactory factory =
            new JwtClaimAuthorizeGatewayFilterFactory(new ObjectMapper());

    private Jwt jwt(String type) {
        return new Jwt("t", Instant.now(), Instant.now().plusSeconds(600),
                Map.of("alg", "RS256"), Map.of("type", type, "roles", List.of("USER")));
    }

    private GatewayFilterChain okChain(AtomicBoolean called) {
        return exchange -> {
            called.set(true);
            return Mono.empty();
        };
    }

    @Test
    void allowsMatchingClaim() {
        var config = new JwtClaimAuthorizeGatewayFilterFactory.Config();
        config.setClaimName("type");
        config.setExpectedValue("access");
        GatewayFilter filter = factory.apply(config);

        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/users/1").build());
        var ctx = new SecurityContextImpl(new JwtAuthenticationToken(jwt("access")));
        var called = new AtomicBoolean(false);

        filter.filter(exchange, okChain(called))
                .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(ctx)))
                .block();
        assertTrue(called.get());
    }

    @Test
    void rejectsWrongClaim() {
        var config = new JwtClaimAuthorizeGatewayFilterFactory.Config();
        config.setClaimName("type");
        config.setExpectedValue("refresh");
        GatewayFilter filter = factory.apply(config);

        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/auth/refresh").build());
        var ctx = new SecurityContextImpl(new JwtAuthenticationToken(jwt("access")));
        var called = new AtomicBoolean(false);

        filter.filter(exchange, okChain(called))
                .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(ctx)))
                .block();
        assertFalse(called.get());
        assertEquals(403, exchange.getResponse().getStatusCode().value());
    }
}
