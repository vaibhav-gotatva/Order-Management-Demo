package com.assignment.demo.specification;

import com.assignment.demo.entity.Order;
import com.assignment.demo.enums.OrderStatus;
import com.assignment.demo.enums.OrderType;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class OrderSpecification {

    private OrderSpecification() {}

    public static Specification<Order> hasUserId(Long userId) {
        return (root, query, cb) ->
                userId == null ? cb.conjunction()
                               : cb.equal(root.get("userId"), userId);
    }

    public static Specification<Order> hasOrderType(OrderType orderType) {
        return (root, query, cb) ->
                orderType == null ? cb.conjunction()
                                  : cb.equal(root.get("orderType"), orderType);
    }

    public static Specification<Order> hasStatus(OrderStatus status) {
        return (root, query, cb) ->
                status == null ? cb.conjunction()
                               : cb.equal(root.get("status"), status);
    }

    public static Specification<Order> createdAfter(LocalDateTime from) {
        return (root, query, cb) ->
                from == null ? cb.conjunction()
                             : cb.greaterThanOrEqualTo(root.get("createdAt"), from);
    }

    public static Specification<Order> createdBefore(LocalDateTime to) {
        return (root, query, cb) ->
                to == null ? cb.conjunction()
                           : cb.lessThanOrEqualTo(root.get("createdAt"), to);
    }

    public static Specification<Order> priceBetween(BigDecimal min, BigDecimal max) {
        return (root, query, cb) -> {
            if (min != null && max != null) {
                return cb.between(root.get("price"), min, max);
            } else if (min != null) {
                return cb.greaterThanOrEqualTo(root.get("price"), min);
            } else if (max != null) {
                return cb.lessThanOrEqualTo(root.get("price"), max);
            }
            return cb.conjunction();
        };
    }

    public static Specification<Order> quantityBetween(Integer min, Integer max) {
        return (root, query, cb) -> {
            if (min != null && max != null) {
                return cb.between(root.get("quantity"), min, max);
            } else if (min != null) {
                return cb.greaterThanOrEqualTo(root.get("quantity"), min);
            } else if (max != null) {
                return cb.lessThanOrEqualTo(root.get("quantity"), max);
            }
            return cb.conjunction();
        };
    }

    public static Specification<Order> buildFrom(
            Long userId,
            OrderType orderType,
            OrderStatus status,
            LocalDateTime createdFrom,
            LocalDateTime createdTo,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Integer minQty,
            Integer maxQty) {

        return Specification
                .where(hasUserId(userId))
                .and(hasOrderType(orderType))
                .and(hasStatus(status))
                .and(createdAfter(createdFrom))
                .and(createdBefore(createdTo))
                .and(priceBetween(minPrice, maxPrice))
                .and(quantityBetween(minQty, maxQty));
    }
}
