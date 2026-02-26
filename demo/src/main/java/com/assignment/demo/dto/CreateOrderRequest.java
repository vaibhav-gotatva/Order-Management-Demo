package com.assignment.demo.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateOrderRequest {
    private String orderType;
    private Integer quantity;
    private BigDecimal price;
    private Long userId;
}
