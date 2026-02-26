package com.assignment.demo.service.impl;

import com.assignment.demo.dto.CreateOrderRequest;
import com.assignment.demo.dto.OrderResponse;
import com.assignment.demo.entity.Order;
import com.assignment.demo.entity.User;
import com.assignment.demo.enums.OrderStatus;
import com.assignment.demo.enums.OrderType;
import com.assignment.demo.repository.OrderRepository;
import com.assignment.demo.repository.UserRepository;
import com.assignment.demo.service.OrderService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest req, Authentication authentication) {

        // 1. Extract caller from SecurityContext (JwtAuthFilter sets a full User entity as principal)
        User caller = (User) authentication.getPrincipal();

        // 2. Determine caller role
        boolean isAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_ADMIN"));

        // 3. Resolve effective userId
        Long effectiveUserId;
        if (isAdmin && req.getUserId() != null) {
            if (!userRepository.existsById(req.getUserId())) {
                throw new EntityNotFoundException("User not found with id: " + req.getUserId());
            }
            effectiveUserId = req.getUserId();
        } else {
            effectiveUserId = caller.getId();
        }

        // 4. Validate orderType
        if (req.getOrderType() == null || req.getOrderType().isBlank()) {
            throw new IllegalArgumentException("orderType is required");
        }
        OrderType orderType;
        try {
            orderType = OrderType.valueOf(req.getOrderType().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid orderType. Accepted values: BUY, SELL");
        }

        // 5. Validate quantity
        if (req.getQuantity() == null) {
            throw new IllegalArgumentException("quantity is required");
        }
        if (req.getQuantity() <= 0) {
            throw new IllegalArgumentException("quantity must be greater than 0");
        }

        // 6. Validate price
        if (req.getPrice() == null) {
            throw new IllegalArgumentException("price is required");
        }
        if (req.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("price must be greater than 0");
        }

        // 7. Build and persist the order
        Order saved = orderRepository.save(Order.builder()
                .orderType(orderType)
                .quantity(req.getQuantity())
                .price(req.getPrice())
                .status(OrderStatus.NEW)
                .userId(effectiveUserId)
                .build());

        // 8. Map to response
        return OrderResponse.builder()
                .orderId(saved.getOrderId())
                .orderType(saved.getOrderType())
                .quantity(saved.getQuantity())
                .price(saved.getPrice())
                .status(saved.getStatus())
                .userId(saved.getUserId())
                .createdAt(saved.getCreatedAt())
                .updatedAt(saved.getUpdatedAt())
                .build();
    }

    @Override
    @Cacheable(value = "orders", key = "#orderId + '_' + #authentication.name")
    public OrderResponse getOrderById(Long orderId, Authentication authentication) {

        // 1. Fetch order from DB (on cache miss)
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with id: " + orderId));

        // 2. Role-based access check
        User caller = (User) authentication.getPrincipal();
        boolean isAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_ADMIN"));

        if (!isAdmin && !order.getUserId().equals(caller.getId())) {
            throw new AccessDeniedException("Access denied");
        }

        // 3. Map to response
        return OrderResponse.builder()
                .orderId(order.getOrderId())
                .orderType(order.getOrderType())
                .quantity(order.getQuantity())
                .price(order.getPrice())
                .status(order.getStatus())
                .userId(order.getUserId())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
