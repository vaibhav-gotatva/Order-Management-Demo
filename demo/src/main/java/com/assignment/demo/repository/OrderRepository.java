package com.assignment.demo.repository;

import com.assignment.demo.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long>,
        JpaSpecificationExecutor<Order> {

    List<Order> findTop10ByUserIdOrderByCreatedAtDesc(Long userId);

    long countByUserId(Long userId);
}
