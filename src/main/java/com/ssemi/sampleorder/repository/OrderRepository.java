package com.ssemi.sampleorder.repository;

import com.ssemi.sampleorder.model.Order;
import com.ssemi.sampleorder.model.OrderStatus;

import java.util.List;
import java.util.Optional;

public interface OrderRepository {
    void save(Order order);
    Optional<Order> findById(String id);
    List<Order> findAll();
    List<Order> findByStatus(OrderStatus status);
    void delete(String id);
}
