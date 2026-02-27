package com.assignment.demo.controller;

import com.assignment.demo.dto.CreateOrderRequest;
import com.assignment.demo.dto.OrderFilterRequest;
import com.assignment.demo.dto.OrderResponse;
import com.assignment.demo.dto.PagedOrderResponse;
import com.assignment.demo.dto.UpdateOrderStatusRequest;
import com.assignment.demo.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Tag(name = "Orders", description = "Create, retrieve, filter, and manage trade orders")
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @Operation(
        summary = "Create a new order",
        description = "Roles: ADMIN, USER. ADMIN can create on behalf of any userId; USER can only create for their own account."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Order created",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = OrderResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input",
            content = @Content(mediaType = "application/json",
                schema = @Schema(example = "{\"error\": \"orderType must be BUY or SELL\"}"))),
        @ApiResponse(responseCode = "403", description = "USER attempted to create order for another user",
            content = @Content(mediaType = "application/json",
                schema = @Schema(example = "{\"error\": \"Access denied\"}")))
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<OrderResponse> createOrder(
            @RequestBody CreateOrderRequest request,
            Authentication authentication) {

        OrderResponse response = orderService.createOrder(request, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
        summary = "Get order by ID",
        description = "Roles: ADMIN, USER. Response is Redis-cached for 60 seconds. USER can only access their own orders."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Order found",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = OrderResponse.class))),
        @ApiResponse(responseCode = "403", description = "USER accessing another user's order",
            content = @Content(mediaType = "application/json",
                schema = @Schema(example = "{\"error\": \"Access denied\"}"))),
        @ApiResponse(responseCode = "404", description = "Order not found",
            content = @Content(mediaType = "application/json",
                schema = @Schema(example = "{\"error\": \"Order not found\"}")))
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<OrderResponse> getOrderById(
            @Parameter(description = "Order ID", required = true, example = "42") @PathVariable Long id,
            Authentication authentication) {

        OrderResponse response = orderService.getOrderById(id, authentication);
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "List orders with filters and pagination",
        description = "Roles: ADMIN, USER. ADMIN can filter by any userId. USER always sees only their own orders (userId param is ignored)."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Paginated order list",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = PagedOrderResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid filter parameters",
            content = @Content(mediaType = "application/json",
                schema = @Schema(example = "{\"error\": \"Invalid status value\"}")))
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<PagedOrderResponse> listOrders(
            @Parameter(description = "Filter by user ID (ADMIN only)", example = "5")
            @RequestParam(required = false) Long userId,

            @Parameter(description = "Filter by order type: BUY or SELL", example = "BUY")
            @RequestParam(required = false) String orderType,

            @Parameter(description = "Filter by status: NEW, PROCESSING, COMPLETED, FAILED, CANCELLED", example = "NEW")
            @RequestParam(required = false) String status,

            @Parameter(description = "Created from (ISO-8601 datetime, e.g. 2025-01-01T00:00:00)", example = "2025-01-01T00:00:00")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdFrom,

            @Parameter(description = "Created to (ISO-8601 datetime, e.g. 2025-12-31T23:59:59)", example = "2025-12-31T23:59:59")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdTo,

            @Parameter(description = "Minimum price (inclusive)", example = "100.00")
            @RequestParam(required = false) BigDecimal minPrice,

            @Parameter(description = "Maximum price (inclusive)", example = "5000.00")
            @RequestParam(required = false) BigDecimal maxPrice,

            @Parameter(description = "Minimum quantity (inclusive)", example = "1")
            @RequestParam(required = false) Integer minQty,

            @Parameter(description = "Maximum quantity (inclusive)", example = "100")
            @RequestParam(required = false) Integer maxQty,

            @Parameter(description = "Page number (0-based, default 0)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size (default 20, max 100)", example = "20")
            @RequestParam(defaultValue = "20") int size,

            @Parameter(description = "Sort field: createdAt, price, quantity, status", example = "createdAt")
            @RequestParam(defaultValue = "createdAt") String sortBy,

            @Parameter(description = "Sort direction: asc or desc", example = "desc")
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

    @Operation(
        summary = "Update order status",
        description = "Role: ADMIN only. Valid transitions: NEW → PROCESSING | CANCELLED; PROCESSING → COMPLETED | FAILED | CANCELLED. Terminal states (COMPLETED, FAILED, CANCELLED) cannot be changed."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Status updated",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = OrderResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid status or illegal transition",
            content = @Content(mediaType = "application/json",
                schema = @Schema(example = "{\"error\": \"Cannot transition from COMPLETED to NEW\"}"))),
        @ApiResponse(responseCode = "403", description = "Not ADMIN",
            content = @Content(mediaType = "application/json",
                schema = @Schema(example = "{\"error\": \"Access denied\"}"))),
        @ApiResponse(responseCode = "404", description = "Order not found",
            content = @Content(mediaType = "application/json",
                schema = @Schema(example = "{\"error\": \"Order not found\"}"))),
        @ApiResponse(responseCode = "409", description = "Conflict while updating",
            content = @Content(mediaType = "application/json",
                schema = @Schema(example = "{\"error\": \"Conflict while updating\"}")))
    })
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @Parameter(description = "Order ID", required = true, example = "42") @PathVariable Long id,
            @RequestBody UpdateOrderStatusRequest request,
            Authentication authentication) {

        return ResponseEntity.ok(orderService.updateOrderStatus(id, request, authentication));
    }

    @Operation(
        summary = "Get order count for a user",
        description = "Roles: ADMIN, USER. USER can only query their own userId."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Order count returned",
            content = @Content(mediaType = "application/json",
                schema = @Schema(example = "{\"userId\": 5, \"count\": 13}"))),
        @ApiResponse(responseCode = "403", description = "USER querying another user's count",
            content = @Content(mediaType = "application/json",
                schema = @Schema(example = "{\"error\": \"Access denied\"}")))
    })
    @GetMapping("/{userId}/order-count")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Map<String, Object>> getUserOrderCount(
            @Parameter(description = "User ID", required = true, example = "5") @PathVariable Long userId,
            Authentication authentication) {

        return ResponseEntity.ok(orderService.countUserOrders(userId, authentication));
    }

    @Operation(
        summary = "Get recent orders for a user",
        description = "Roles: ADMIN, USER. USER can only query their own userId. Returns the most recent orders for the specified user."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "List of recent orders",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = OrderResponse.class))),
        @ApiResponse(responseCode = "403", description = "USER querying another user's orders",
            content = @Content(mediaType = "application/json",
                schema = @Schema(example = "{\"error\": \"Access denied\"}")))
    })
    @GetMapping("/{userId}/recent-orders")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<OrderResponse>> getUserRecentOrders(
            @Parameter(description = "User ID", required = true, example = "5") @PathVariable Long userId,
            Authentication authentication) {

        return ResponseEntity.ok(orderService.getRecentOrdersForUser(userId, authentication));
    }
}
