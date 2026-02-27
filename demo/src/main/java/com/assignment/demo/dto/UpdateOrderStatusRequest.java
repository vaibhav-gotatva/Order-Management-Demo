package com.assignment.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Payload to update an order's status (ADMIN only)")
public class UpdateOrderStatusRequest {

    @Schema(
        description = "Target status. Valid transitions: NEW→PROCESSING|CANCELLED; PROCESSING→COMPLETED|FAILED|CANCELLED. Terminal states cannot be changed.",
        example = "PROCESSING",
        allowableValues = {"NEW", "PROCESSING", "COMPLETED", "FAILED", "CANCELLED"}
    )
    private String status;
}
