package com.ssemi.sampleorder.service;

import com.ssemi.sampleorder.model.*;
import com.ssemi.sampleorder.repository.OrderRepository;
import com.ssemi.sampleorder.repository.SampleRepository;

import java.util.List;
import java.util.UUID;

public class OrderService {

    private final OrderRepository orderRepository;
    private final SampleRepository sampleRepository;
    private final ProductionQueue productionQueue;

    public OrderService(OrderRepository orderRepository,
                        SampleRepository sampleRepository,
                        ProductionQueue productionQueue) {
        this.orderRepository = orderRepository;
        this.sampleRepository = sampleRepository;
        this.productionQueue = productionQueue;
    }

    public Order reserveOrder(String sampleId, String customerName, int quantity) {
        // Order 생성자가 customerName blank/null, quantity<=0 유효성 검증 수행
        Order order = new Order(UUID.randomUUID().toString(), sampleId, customerName, quantity);
        orderRepository.save(order);
        return order;
    }

    public Order approveOrder(String orderId) {
        if (orderId == null) throw new IllegalArgumentException("orderId는 null일 수 없습니다.");
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다: " + orderId));

        if (order.getStatus() != OrderStatus.RESERVED)
            throw new IllegalStateException("RESERVED 상태의 주문만 승인할 수 있습니다. 현재 상태: " + order.getStatus());

        Sample sample = sampleRepository.findById(order.getSampleId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 시료입니다: " + order.getSampleId()));

        if (sample.getStock() >= order.getQuantity()) {
            sample.reduceStock(order.getQuantity());
            order.transitionTo(OrderStatus.CONFIRMED);
            sampleRepository.save(sample);
        } else {
            order.transitionTo(OrderStatus.PRODUCING);
            productionQueue.enqueue(order);
        }

        orderRepository.save(order);
        return order;
    }

    public Order rejectOrder(String orderId) {
        if (orderId == null) throw new IllegalArgumentException("orderId는 null일 수 없습니다.");
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다: " + orderId));

        if (order.getStatus() != OrderStatus.RESERVED)
            throw new IllegalStateException("RESERVED 상태의 주문만 거절할 수 있습니다. 현재 상태: " + order.getStatus());

        order.transitionTo(OrderStatus.REJECTED);
        orderRepository.save(order);
        return order;
    }

    public List<Order> listReservedOrders() {
        return orderRepository.findByStatus(OrderStatus.RESERVED);
    }

    public List<Order> listConfirmedOrders() {
        return orderRepository.findByStatus(OrderStatus.CONFIRMED);
    }

    public Order releaseOrder(String orderId) {
        if (orderId == null) throw new IllegalArgumentException("orderId는 null일 수 없습니다.");
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다: " + orderId));

        if (order.getStatus() != OrderStatus.CONFIRMED)
            throw new IllegalStateException("CONFIRMED 상태의 주문만 출고할 수 있습니다. 현재 상태: " + order.getStatus());

        order.transitionTo(OrderStatus.RELEASED);
        orderRepository.save(order);
        return order;
    }
}
