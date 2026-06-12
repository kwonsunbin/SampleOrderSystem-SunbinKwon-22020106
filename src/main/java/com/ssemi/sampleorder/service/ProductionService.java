package com.ssemi.sampleorder.service;

import com.ssemi.sampleorder.model.*;
import com.ssemi.sampleorder.repository.OrderRepository;
import com.ssemi.sampleorder.repository.SampleRepository;

import java.util.List;

public class ProductionService {

    private final OrderRepository orderRepository;
    private final SampleRepository sampleRepository;
    private final ProductionQueue productionQueue;

    public ProductionService(OrderRepository orderRepository,
                             SampleRepository sampleRepository,
                             ProductionQueue productionQueue) {
        this.orderRepository = orderRepository;
        this.sampleRepository = sampleRepository;
        this.productionQueue = productionQueue;
    }

    // 실 생산량 = ceil(부족분 / (수율 * 0.9))
    public int calculateActualProduction(int shortage, double yield) {
        if (shortage <= 0)
            throw new IllegalArgumentException("부족분은 1 이상이어야 합니다: " + shortage);
        if (yield <= 0.0 || yield > 1.0)
            throw new IllegalArgumentException("수율은 0 초과 1 이하여야 합니다: " + yield);
        return (int) Math.ceil(shortage / (yield * 0.9));
    }

    // 총 생산시간 = 평균 생산시간 * 실 생산량
    public int calculateTotalProductionTime(int avgProductionTime, int actualProduction) {
        return avgProductionTime * actualProduction;
    }

    public Order processNext() {
        if (productionQueue.isEmpty())
            throw new IllegalStateException("생산 큐가 비어 있습니다.");

        Order order = productionQueue.dequeue();
        Sample sample = sampleRepository.findById(order.getSampleId())
                .orElseThrow(() -> new IllegalArgumentException("시료를 찾을 수 없습니다: " + order.getSampleId()));

        int shortage = Math.max(0, order.getQuantity() - sample.getStock());
        if (shortage > 0) {
            int actualProduction = calculateActualProduction(shortage, sample.getYield());
            sample.addStock(actualProduction);
        }
        sample.reduceStock(order.getQuantity());
        order.transitionTo(OrderStatus.CONFIRMED);

        sampleRepository.save(sample);
        orderRepository.save(order);
        return order;
    }

    public List<Order> getQueueSnapshot() {
        return productionQueue.toList();
    }
}
