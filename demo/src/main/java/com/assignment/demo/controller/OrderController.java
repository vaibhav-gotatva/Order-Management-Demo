package com.assignment.demo.controller;

import com.assignment.demo.dto.CreateOrderRequest;
import com.assignment.demo.dto.OrderFilterRequest;
import com.assignment.demo.dto.OrderResponse;
import com.assignment.demo.dto.PagedOrderResponse;
import com.assignment.demo.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<OrderResponse> createOrder(
            @RequestBody CreateOrderRequest request,
            Authentication authentication) {

        OrderResponse response = orderService.createOrder(request, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<OrderResponse> getOrderById(
            @PathVariable Long id,
            Authentication authentication) {

        OrderResponse response = orderService.getOrderById(id, authentication);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'VIEWER')")
    public ResponseEntity<PagedOrderResponse> listOrders(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String orderType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdTo,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Integer minQty,
            @RequestParam(required = false) Integer maxQty,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            Authentication authentication) {

        OrderFilterRequest filter = new OrderFilterRequest();
        filter.setUserId(userId);
        filter.setOrderType(orderType);
        filter.setStatus(status);
        filter.setCreatedFrom(createdFrom);
        filter.setCreatedTo(createdTo);
        filter.setMinPrice(minPrice);
        filter.setMaxPrice(maxPrice);
        filter.setMinQty(minQty);
        filter.setMaxQty(maxQty);
        filter.setPage(page);
        filter.setSize(size);
        filter.setSortBy(sortBy);
        filter.setSortDir(sortDir);

        return ResponseEntity.ok(orderService.listOrders(filter, authentication));
    }
}
