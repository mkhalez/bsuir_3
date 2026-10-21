package com.shoplab.order.config;

import com.shoplab.order.client.UserServiceClient;
import java.net.http.HttpClient;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.*;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
@RequiredArgsConstructor
public class ClientConfig {

    private final ClientProperties props;

    @Bean
    public UserServiceClient userServiceClient(RestClient.Builder builder) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(props.connectionTimeout())).build();
        JdkClientHttpRequestFactory rf = new JdkClientHttpRequestFactory(httpClient);
        rf.setReadTimeout(Duration.ofMillis(props.readTimeout()));
        RestClient rest = builder.baseUrl(props.baseUrl()).requestFactory(rf)
                .requestInterceptor((req, body, exec) -> {
                    var auth = SecurityContextHolder.getContext().getAuthentication();
                    if (auth instanceof JwtAuthenticationToken jwt) {
                        req.getHeaders().setBearerAuth(jwt.getToken().getTokenValue());
                    }
                    return exec.execute(req, body);
                }).build();
        return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(rest)).build().createClient(UserServiceClient.class);
    }
}
