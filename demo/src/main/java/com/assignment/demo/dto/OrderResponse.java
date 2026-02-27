package com.assignment.demo.dto;

import com.assignment.demo.enums.OrderStatus;
import com.assignment.demo.enums.OrderType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Representation of a single trade order")
public class OrderResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "Unique order identifier", example = "42")
    private Long orderId;

    @Schema(description = "Order type", example = "BUY")
    private OrderType orderType;

    @Schema(description = "Number of units", example = "10")
    private Integer quantity;

    @Schema(description = "Price per unit", example = "250.00")
    private BigDecimal price;

    @Schema(description = "Current status of the order", example = "NEW")
    private OrderStatus status;

    @Schema(description = "ID of the user who owns this order", example = "5")
    private Long userId;

    @Schema(description = "Timestamp when the order was created", example = "2025-06-01T10:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "Timestamp when the order was last updated", example = "2025-06-01T10:05:00")
    private LocalDateTime updatedAt;
}
