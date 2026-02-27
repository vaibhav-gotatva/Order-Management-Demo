package com.assignment.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "Payload to create a new trade order")
public class CreateOrderRequest {

    @NotBlank(message = "orderType is required")
    @Schema(description = "Order type", example = "BUY", allowableValues = {"BUY", "SELL"})
    private String orderType;

    @NotNull(message = "quantity is required")
    @Positive(message = "quantity must be greater than 0")
    @Schema(description = "Number of units", example = "10")
    private Integer quantity;

    @NotNull(message = "price is required")
    @Positive(message = "price must be greater than 0")
    @Schema(description = "Price per unit", example = "250.00")
    private BigDecimal price;

    @Schema(description = "ID of the user placing the order", example = "5")
    private Long userId;
}
