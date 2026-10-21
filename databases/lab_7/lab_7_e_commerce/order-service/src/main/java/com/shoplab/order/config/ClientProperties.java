package com.shoplab.order.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "user-service")
public record ClientProperties(String baseUrl, int connectionTimeout, int readTimeout) {
}
