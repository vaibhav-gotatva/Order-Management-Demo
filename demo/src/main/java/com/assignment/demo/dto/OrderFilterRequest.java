package com.assignment.demo.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderFilterRequest {

    // ADMIN only — USER has this silently ignored
    private Long userId;

    // Enum filters — accepted as String from query params, parsed and validated in service
    private String orderType;
    private String status;

    // Date range
    private LocalDateTime createdFrom;
    private LocalDateTime createdTo;

    // Price range
    private BigDecimal minPrice;
    private BigDecimal maxPrice;

    // Quantity range
    private Integer minQty;
    private Integer maxQty;

    // Pagination
    private int page = 0;
    private int size = 20;

    // Sorting
    private String sortBy = "createdAt";
    private String sortDir = "desc";
}
