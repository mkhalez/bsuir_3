package com.shoplab.gateway.filters;

import tools.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.*;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.*;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class JwtClaimAuthorizeGatewayFilterFactory extends AbstractGatewayFilterFactory<JwtClaimAuthorizeGatewayFilterFactory.Config> {

    private final ObjectMapper objectMapper;

    public JwtClaimAuthorizeGatewayFilterFactory(ObjectMapper objectMapper) {
        super(Config.class);
        this.objectMapper = objectMapper;
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return List.of("claimName", "expectedValue");
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication())
                .filter(a -> a instanceof JwtAuthenticationToken)
                .map(a -> ((JwtAuthenticationToken) a).getToken())
                .flatMap(jwt -> {
                    if (config.expectedValue.equals(jwt.getClaimAsString(config.claimName))) {
                        return chain.filter(exchange);
                    }
                    var response = exchange.getResponse();
                    response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                    response.setStatusCode(HttpStatus.FORBIDDEN);
                    byte[] bytes;
                    try {
                        bytes = objectMapper.writeValueAsString("token should be " + config.expectedValue).getBytes(StandardCharsets.UTF_8);
                    } catch (Exception e) {
                        bytes = ("token should be " + config.expectedValue).getBytes(StandardCharsets.UTF_8);
                    }
                    DataBuffer buffer = response.bufferFactory().wrap(bytes);
                    return response.writeWith(Mono.just(buffer));
                });
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Config {
        private String claimName;
        private String expectedValue;
    }
}
