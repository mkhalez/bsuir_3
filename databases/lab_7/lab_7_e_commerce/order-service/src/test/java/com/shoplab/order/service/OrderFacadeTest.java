package com.shoplab.order.service;

import com.shoplab.order.client.BuyerProfile;
import com.shoplab.order.client.UserServiceClient;
import com.shoplab.order.domain.Order;
import com.shoplab.order.domain.Product;
import com.shoplab.order.dto.CreateOrderRequest;
import com.shoplab.order.repo.OrderJdbcRepository;
import com.shoplab.order.repo.ProductJdbcRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class OrderFacadeTest {

    @Mock UserServiceClient userClient;
    @Mock OrderJdbcRepository orders;
    @Mock ProductJdbcRepository products;

    @InjectMocks OrderFacade facade;

    private BuyerProfile buyer() {
        return new BuyerProfile(1L, 10L, "Ivan", "Petrov", "ivan@shop.by", null, LocalDate.of(1998, 1, 1), true, OffsetDateTime.now());
    }

    private Product product() {
        return new Product(5L, 1L, "Audio", "JBL-FLIP6", "JBL Flip 6", new BigDecimal("499.00"), 100, true, OffsetDateTime.now());
    }

    @Test
    void createValidatesBuyerAndDecreasesStock() {
        Mockito.when(userClient.findByEmail("ivan@shop.by")).thenReturn(buyer());
        Mockito.when(products.findById(5L)).thenReturn(Optional.of(product()));
        Mockito.when(orders.insertOrder(eq(1L), eq("ivan@shop.by"), any())).thenReturn(9L);
        Mockito.when(products.decreaseStock(5L, 2)).thenReturn(true);
        Mockito.when(orders.findDetailedById(9L)).thenReturn(Optional.of(
                new Order(9L, 1L, "ivan@shop.by", "CREATED", new BigDecimal("998.00"), false, OffsetDateTime.now(), List.of())));

        var order = facade.create(new CreateOrderRequest("ivan@shop.by", List.of(new CreateOrderRequest.OrderLineRequest(5L, 2))), "ivan@shop.by");

        assertEquals(9L, order.id());
        Mockito.verify(products).decreaseStock(5L, 2);
        Mockito.verify(orders).insertItem(eq(9L), eq(5L), eq(2), eq(new BigDecimal("499.00")));
    }

    @Test
    void createInactiveBuyerFails() {
        var blocked = new BuyerProfile(1L, 10L, "I", "P", "ivan@shop.by", null, LocalDate.of(1998, 1, 1), false, OffsetDateTime.now());
        Mockito.when(userClient.findByEmail("ivan@shop.by")).thenReturn(blocked);
        assertThrows(IllegalStateException.class, () ->
                facade.create(new CreateOrderRequest("ivan@shop.by", List.of(new CreateOrderRequest.OrderLineRequest(5L, 1))), "x"));
    }

    @Test
    void cancelReturnsStock() {
        var full = new Order(9L, 1L, "ivan@shop.by", "PAID", new BigDecimal("499.00"), false, OffsetDateTime.now(),
                List.of(new com.shoplab.order.domain.OrderItem(1L, 9L, 5L, "JBL", 1, new BigDecimal("499.00"), new BigDecimal("499.00"))));
        Mockito.when(orders.statusOf(9L)).thenReturn(Optional.of("PAID"));
        Mockito.when(orders.findDetailedById(9L)).thenReturn(Optional.of(full));

        facade.changeStatus(9L, "CANCELLED", "admin", true);

        Mockito.verify(products).increaseStock(5L, 1);
        Mockito.verify(orders).updateStatus(9L, "CANCELLED");
    }
}
