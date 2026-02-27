package com.assignment.demo.service;

import com.assignment.demo.dto.CreateOrderRequest;
import com.assignment.demo.dto.OrderFilterRequest;
import com.assignment.demo.dto.OrderResponse;
import com.assignment.demo.dto.PagedOrderResponse;
import com.assignment.demo.dto.UpdateOrderStatusRequest;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Map;

public interface OrderService {
    OrderResponse createOrder(CreateOrderRequest req, Authentication authentication);
    OrderResponse getOrderById(Long orderId, Authentication authentication);
    PagedOrderResponse listOrders(OrderFilterRequest filter, Authentication authentication);
    OrderResponse updateOrderStatus(Long orderId, UpdateOrderStatusRequest request, Authentication authentication);
    Map<String, Object> countUserOrders(Long userId, Authentication authentication);
    List<OrderResponse> getRecentOrdersForUser(Long userId, Authentication authentication);
}
