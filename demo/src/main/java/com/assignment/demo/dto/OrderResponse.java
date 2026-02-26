package com.assignment.demo.dto;

import com.assignment.demo.enums.OrderStatus;
import com.assignment.demo.enums.OrderType;
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
public class OrderResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long orderId;
    private OrderType orderType;
    private Integer quantity;
    private BigDecimal price;
    private OrderStatus status;
    private Long userId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
