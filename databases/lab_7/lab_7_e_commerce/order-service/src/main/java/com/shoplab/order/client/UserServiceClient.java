package com.shoplab.order.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;

@CircuitBreaker(name = "UserServiceCB")
public interface UserServiceClient {

    @GetExchange("/by-email")
    BuyerProfile findByEmail(@RequestParam("email") String email);

    @GetExchange("/{id}")
    BuyerProfile findById(@PathVariable("id") long id);
}
