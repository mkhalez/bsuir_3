package com.shoplab.user.client;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class AuthInternalClient {

    private final RestClient.Builder builder;

    @Value("${auth.base-url:http://localhost:8081/internal}")
    private String baseUrl = "http://localhost:8081/internal";

    private RestClient client() {
        return builder.baseUrl(baseUrl).build();
    }

    public void disable(long authUserId) {
        client().patch().uri("/users/{id}/disable", authUserId).retrieve().toBodilessEntity();
    }

    public void enable(long authUserId) {
        client().patch().uri("/users/{id}/enable", authUserId).retrieve().toBodilessEntity();
    }

    public void delete(long authUserId) {
        client().delete().uri("/users/{id}", authUserId).retrieve().toBodilessEntity();
    }
}
