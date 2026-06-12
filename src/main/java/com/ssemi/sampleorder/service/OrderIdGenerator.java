package com.ssemi.sampleorder.service;

import com.ssemi.sampleorder.model.Order;
import com.ssemi.sampleorder.repository.OrderRepository;

public class OrderIdGenerator {

    private static final String PREFIX = "ORD-";

    private final OrderRepository orderRepository;

    public OrderIdGenerator(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public String generate() {
        int max = orderRepository.findAll().stream()
                .map(Order::getId)
                .filter(id -> id.startsWith(PREFIX))
                .mapToInt(id -> {
                    try {
                        return Integer.parseInt(id.substring(PREFIX.length()));
                    } catch (NumberFormatException e) {
                        return 0;
                    }
                })
                .max()
                .orElse(0);
        return PREFIX + String.format("%04d", max + 1);
    }
}
