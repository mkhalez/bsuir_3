package com.shoplab.order.web;

import com.shoplab.order.domain.Order;
import com.shoplab.order.repo.OrderJdbcRepository;
import com.shoplab.order.service.OrderFacade;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock OrderFacade facade;
    @Mock OrderJdbcRepository orders;

    MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.standaloneSetup(new OrderController(facade, orders))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void byIdReturnsOrder() throws Exception {
        var order = new Order(1L, 1L, "ivan@shop.by", "PAID", new BigDecimal("499.00"), false, OffsetDateTime.now(), List.of());
        Mockito.when(orders.findDetailedById(1L)).thenReturn(Optional.of(order));
        mvc.perform(get("/api/orders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.buyerEmail").value("ivan@shop.by"));
    }

    @Test
    void byIdMissingReturns404() throws Exception {
        Mockito.when(orders.findDetailedById(999L)).thenReturn(Optional.empty());
        mvc.perform(get("/api/orders/999")).andExpect(status().isNotFound());
    }
}
