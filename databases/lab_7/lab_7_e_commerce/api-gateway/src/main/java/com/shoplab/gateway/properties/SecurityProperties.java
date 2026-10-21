package com.shoplab.gateway.properties;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app")
public class SecurityProperties {
    private List<String> permitAllRoutes;
    private List<String> authenticatedRoutes;
}
