package com.assignment.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "Payload to create a new trade order")
public class CreateOrderRequest {

    @Schema(description = "Order type", example = "BUY", allowableValues = {"BUY", "SELL"})
    private String orderType;

    @Schema(description = "Number of units", example = "10")
    private Integer quantity;

    @Schema(description = "Price per unit", example = "250.00")
    private BigDecimal price;

    @Schema(description = "ID of the user placing the order", example = "5")
    private Long userId;
}
