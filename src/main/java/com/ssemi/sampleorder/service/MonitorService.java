package com.ssemi.sampleorder.service;

import com.ssemi.sampleorder.model.*;
import com.ssemi.sampleorder.repository.OrderRepository;
import com.ssemi.sampleorder.repository.SampleRepository;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MonitorService {

    private final OrderRepository orderRepository;
    private final SampleRepository sampleRepository;

    public MonitorService(OrderRepository orderRepository, SampleRepository sampleRepository) {
        this.orderRepository = orderRepository;
        this.sampleRepository = sampleRepository;
    }

    public Map<OrderStatus, Long> getOrderCountByStatus() {
        Map<OrderStatus, Long> counts = orderRepository.findAll().stream()
                .filter(o -> o.getStatus() != OrderStatus.REJECTED)
                .collect(Collectors.groupingBy(Order::getStatus, Collectors.counting()));

        for (OrderStatus status : new OrderStatus[]{
                OrderStatus.RESERVED, OrderStatus.PRODUCING,
                OrderStatus.CONFIRMED, OrderStatus.RELEASED}) {
            counts.putIfAbsent(status, 0L);
        }
        return counts;
    }

    public List<Order> getOrdersByStatus(OrderStatus status) {
        if (status == null)
            throw new IllegalArgumentException("status는 null일 수 없습니다.");
        if (status == OrderStatus.REJECTED)
            throw new IllegalArgumentException("REJECTED 상태는 모니터링 대상이 아닙니다.");
        return orderRepository.findByStatus(status);
    }

    public List<SampleStockInfo> getStockStatusBySample() {
        Map<String, Integer> demandBySample = orderRepository.findByStatus(OrderStatus.PRODUCING)
                .stream()
                .collect(Collectors.groupingBy(Order::getSampleId,
                        Collectors.summingInt(Order::getQuantity)));

        return sampleRepository.findAll().stream()
                .map(sample -> {
                    int demand = demandBySample.getOrDefault(sample.getId(), 0);
                    StockStatus stockStatus = StockStatus.of(sample.getStock(), demand);
                    return new SampleStockInfo(
                            sample.getId(),
                            sample.getName(),
                            sample.getStock(),
                            demand,
                            stockStatus);
                })
                .collect(Collectors.toList());
    }
}
