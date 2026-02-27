package com.assignment.demo.service.impl;

import com.assignment.demo.dto.CreateOrderRequest;
import com.assignment.demo.dto.OrderFilterRequest;
import com.assignment.demo.dto.OrderResponse;
import com.assignment.demo.dto.PagedOrderResponse;
import com.assignment.demo.dto.UpdateOrderStatusRequest;
import com.assignment.demo.entity.Order;
import com.assignment.demo.entity.User;
import com.assignment.demo.enums.OrderStatus;
import com.assignment.demo.enums.OrderType;
import com.assignment.demo.repository.OrderRepository;
import com.assignment.demo.repository.UserRepository;
import com.assignment.demo.service.OrderService;
import com.assignment.demo.service.UserRedisService;
import com.assignment.demo.specification.OrderSpecification;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final CacheManager cacheManager;
    private final UserRedisService userRedisService;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "createdAt", "updatedAt", "price", "quantity", "orderId", "status", "orderType"
    );

    @Override
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest req, Authentication authentication) {

        // 1. Extract caller from SecurityContext (JwtAuthFilter sets a full User entity as principal)
        User caller = (User) authentication.getPrincipal();

        // 2. Determine caller role
        boolean isAdmin = hasRole(authentication, "ROLE_ADMIN");

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

        OrderResponse response = toResponse(saved);

        // 8. Update Redis counter and recent-orders list (best-effort; failures are logged, never thrown)
        userRedisService.incrementOrderCount(effectiveUserId);
        userRedisService.pushRecentOrder(effectiveUserId, response);

        return response;
    }

    @Override
    @Cacheable(value = "orders", key = "#orderId")
    public OrderResponse getOrderById(Long orderId, Authentication authentication) {

        // 1. Fetch order from DB (on cache miss)
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with id: " + orderId));

        // 2. Role-based access check
        User caller = (User) authentication.getPrincipal();
        boolean isAdmin = hasRole(authentication, "ROLE_ADMIN");

        if (!isAdmin && !order.getUserId().equals(caller.getId())) {
            throw new AccessDeniedException("Access denied");
        }

        return toResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedOrderResponse listOrders(OrderFilterRequest filter, Authentication authentication) {

        // 1. Resolve caller identity and role
        User caller = (User) authentication.getPrincipal();
        boolean isAdmin = hasRole(authentication, "ROLE_ADMIN");
        boolean isViewer = hasRole(authentication, "ROLE_VIEWER");

        // 2. Enforce userId scoping
        Long effectiveUserId;
        if (isAdmin) {
            // ADMIN: may optionally filter by userId; validate it exists if provided
            if (filter.getUserId() != null && !userRepository.existsById(filter.getUserId())) {
                throw new EntityNotFoundException("User not found with id: " + filter.getUserId());
            }
            effectiveUserId = filter.getUserId(); // null = no filter, all users
        } else if (isViewer) {
            // VIEWER: always sees all orders; userId param is silently ignored
            effectiveUserId = null;
        } else {
            // USER: always scoped to their own orders; userId param is silently ignored
            effectiveUserId = caller.getId();
        }

        // 3. Parse and validate orderType
        OrderType parsedOrderType = null;
        if (filter.getOrderType() != null && !filter.getOrderType().isBlank()) {
            try {
                parsedOrderType = OrderType.valueOf(filter.getOrderType().trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid orderType. Accepted values: BUY, SELL");
            }
        }

        // 4. Parse and validate status
        OrderStatus parsedStatus = null;
        if (filter.getStatus() != null && !filter.getStatus().isBlank()) {
            try {
                parsedStatus = OrderStatus.valueOf(filter.getStatus().trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid status. Accepted values: " +
                        Arrays.stream(OrderStatus.values()).map(Enum::name).collect(Collectors.joining(", ")));
            }
        }

        // 5. Validate date range
        if (filter.getCreatedFrom() != null && filter.getCreatedTo() != null
                && filter.getCreatedFrom().isAfter(filter.getCreatedTo())) {
            throw new IllegalArgumentException("createdFrom must not be after createdTo");
        }

        // 6. Validate price range
        if (filter.getMinPrice() != null && filter.getMinPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("minPrice must be >= 0");
        }
        if (filter.getMaxPrice() != null && filter.getMaxPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("maxPrice must be >= 0");
        }
        if (filter.getMinPrice() != null && filter.getMaxPrice() != null
                && filter.getMinPrice().compareTo(filter.getMaxPrice()) > 0) {
            throw new IllegalArgumentException("minPrice must not be greater than maxPrice");
        }

        // 7. Validate quantity range
        if (filter.getMinQty() != null && filter.getMinQty() < 0) {
            throw new IllegalArgumentException("minQty must be >= 0");
        }
        if (filter.getMaxQty() != null && filter.getMaxQty() < 0) {
            throw new IllegalArgumentException("maxQty must be >= 0");
        }
        if (filter.getMinQty() != null && filter.getMaxQty() != null
                && filter.getMinQty() > filter.getMaxQty()) {
            throw new IllegalArgumentException("minQty must not be greater than maxQty");
        }

        // 8. Validate pagination
        if (filter.getPage() < 0) {
            throw new IllegalArgumentException("page must be >= 0");
        }
        if (filter.getSize() < 1 || filter.getSize() > 100) {
            throw new IllegalArgumentException("size must be between 1 and 100");
        }

        // 9. Build Sort — whitelist allowed sort fields; unknown values fall back to createdAt
        String sortBy = (filter.getSortBy() != null && ALLOWED_SORT_FIELDS.contains(filter.getSortBy()))
                ? filter.getSortBy()
                : "createdAt";
        Sort.Direction direction = "asc".equalsIgnoreCase(filter.getSortDir())
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(filter.getPage(), filter.getSize(), direction, sortBy);

        // 10. Build Specification and query
        Specification<Order> spec = OrderSpecification.buildFrom(
                effectiveUserId,
                parsedOrderType,
                parsedStatus,
                filter.getCreatedFrom(),
                filter.getCreatedTo(),
                filter.getMinPrice(),
                filter.getMaxPrice(),
                filter.getMinQty(),
                filter.getMaxQty()
        );

        Page<Order> resultPage = orderRepository.findAll(spec, pageable);

        // Validate requested page is within bounds (totalPages only known after query)
        if (filter.getPage() > 0 && filter.getPage() >= resultPage.getTotalPages()) {
            throw new IllegalArgumentException(
                    "Page index " + filter.getPage() + " out of bounds. Total pages: " + resultPage.getTotalPages()
            );
        }

        // 11. Map to response
        List<OrderResponse> content = resultPage.getContent().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return PagedOrderResponse.builder()
                .content(content)
                .page(resultPage.getNumber())
                .size(resultPage.getSize())
                .totalElements(resultPage.getTotalElements())
                .totalPages(resultPage.getTotalPages())
                .last(resultPage.isLast())
                .build();
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, UpdateOrderStatusRequest request, Authentication authentication) {

        // 1. Validate request body
        if (request == null || request.getStatus() == null || request.getStatus().isBlank()) {
            throw new IllegalArgumentException("status is required");
        }

        // 2. Parse and validate the requested new status
        OrderStatus newStatus;
        try {
            newStatus = OrderStatus.valueOf(request.getStatus().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status. Accepted values: " +
                    Arrays.stream(OrderStatus.values()).map(Enum::name).collect(Collectors.joining(", ")));
        }

        // 3. Fetch the order (throws 404 if not found)
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with id: " + orderId));

        // 4. Validate state transition
        if (!order.getStatus().canTransitionTo(newStatus)) {
            throw new IllegalArgumentException(
                    "Invalid status transition: " + order.getStatus() + " → " + newStatus +
                    ". Allowed transitions from " + order.getStatus() + ": " +
                    order.getStatus().allowedTransitions()
            );
        }

        // 5. Apply update — @Version causes ObjectOptimisticLockingFailureException on concurrent write → 409
        order.setStatus(newStatus);
        Order saved = orderRepository.save(order);

        // 6. Evict the single-order cache entry and invalidate the user's recent-orders list
        evictOrderCache(orderId);
        userRedisService.invalidateRecentOrders(saved.getUserId());

        return toResponse(saved);
    }

    @Override
    public Map<String, Object> countUserOrders(Long userId, Authentication authentication) {
        User caller = (User) authentication.getPrincipal();
        boolean isAdmin = hasRole(authentication, "ROLE_ADMIN");

        if (!isAdmin && !caller.getId().equals(userId)) {
            throw new AccessDeniedException("Access denied");
        }
        if (isAdmin && !userRepository.existsById(userId)) {
            throw new EntityNotFoundException("User not found with id: " + userId);
        }

        Long count = userRedisService.getOrderCount(userId);
        if (count == null) {
            log.warn("Redis miss for order count, userId '{}': falling back to DB and re-seeding Redis", userId);
            count = orderRepository.countByUserId(userId);
            // Re-seed Redis with the accurate DB count so subsequent reads are served from Redis
            userRedisService.seedOrderCount(userId, count);
        }

        return Map.of("userId", userId, "orderCount", count);
    }

    @Override
    public List<OrderResponse> getRecentOrdersForUser(Long userId, Authentication authentication) {
        User caller = (User) authentication.getPrincipal();
        boolean isAdmin = hasRole(authentication, "ROLE_ADMIN");

        if (!isAdmin && !caller.getId().equals(userId)) {
            throw new AccessDeniedException("Access denied");
        }
        if (isAdmin && !userRepository.existsById(userId)) {
            throw new EntityNotFoundException("User not found with id: " + userId);
        }

        List<OrderResponse> cached = userRedisService.getRecentOrders(userId);
        if (cached != null) {
            return cached;
        }

        log.warn("Redis miss for recent orders, userId '{}': falling back to DB", userId);
        List<Order> orders = orderRepository.findTop10ByUserIdOrderByCreatedAtDesc(userId);
        List<OrderResponse> result = orders.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        userRedisService.repopulateRecentOrders(userId, result);

        return result;
    }

    private void evictOrderCache(Long orderId) {
        try {
            Cache ordersCache = cacheManager.getCache("orders");
            if (ordersCache == null) return;
            ordersCache.evict(orderId);
        } catch (RuntimeException e) {
            log.warn("Redis EVICT error for order '{}': {}", orderId, e.getMessage());
        }
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals(role));
    }

    private OrderResponse toResponse(Order order) {
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
