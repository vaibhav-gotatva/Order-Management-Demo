package com.assignment.demo.service;

import com.assignment.demo.dto.CreateOrderRequest;
import com.assignment.demo.dto.OrderResponse;
import org.springframework.security.core.Authentication;

public interface OrderService {
    OrderResponse createOrder(CreateOrderRequest req, Authentication authentication);
    OrderResponse getOrderById(Long orderId, Authentication authentication);
}
