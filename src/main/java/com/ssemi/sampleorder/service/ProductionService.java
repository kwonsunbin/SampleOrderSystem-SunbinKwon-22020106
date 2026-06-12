package com.ssemi.sampleorder.service;

import com.ssemi.sampleorder.model.*;
import com.ssemi.sampleorder.repository.OrderRepository;
import com.ssemi.sampleorder.repository.SampleRepository;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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
        long result = (long) Math.ceil(shortage / (yield * 0.9));
        if (result > Integer.MAX_VALUE)
            throw new ArithmeticException("실 생산량이 int 범위를 초과합니다: " + result);
        return (int) result;
    }

    // 총 생산시간(초) = 평균 생산시간(초) * 실 생산량
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

    // RESERVED → PRODUCING: 단일 생산 라인 시작
    public Order startNextProduction(Clock clock) {
        if (!orderRepository.findByStatus(OrderStatus.PRODUCING).isEmpty())
            throw new IllegalStateException("이미 생산 중인 주문이 있습니다.");
        if (productionQueue.isEmpty())
            throw new IllegalStateException("생산 큐가 비어 있습니다.");

        Order order = productionQueue.dequeue();
        Sample sample = sampleRepository.findById(order.getSampleId())
                .orElseThrow(() -> new IllegalArgumentException("시료를 찾을 수 없습니다: " + order.getSampleId()));

        int shortage = Math.max(0, order.getQuantity() - sample.getStock());
        int unitsToMake = shortage > 0
                ? calculateActualProduction(shortage, sample.getYield())
                : order.getQuantity();
        int totalSeconds = calculateTotalProductionTime(sample.getAvgProductionTime(), unitsToMake);

        order.transitionTo(OrderStatus.PRODUCING);
        order.startProduction(LocalDateTime.now(clock), totalSeconds);
        orderRepository.save(order);
        return order;
    }

    // 경과 시간(초) 체크 후 생산 완료 처리: PRODUCING → CONFIRMED, 재고 반영
    public void completeProductionIfReady(Clock clock) {
        List<Order> producing = orderRepository.findByStatus(OrderStatus.PRODUCING);
        if (producing.isEmpty()) return;

        Order order = producing.get(0);
        LocalDateTime startedAt = order.getProductionStartedAt();
        if (startedAt == null) return;

        long elapsed = Duration.between(startedAt, LocalDateTime.now(clock)).toSeconds();
        if (elapsed < order.getTotalProductionSeconds()) return;

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
    }

    // 현재 PRODUCING 주문의 진행 정보 반환
    public Optional<ProductionProgressInfo> getCurrentlyProducingInfo(Clock clock) {
        List<Order> producing = orderRepository.findByStatus(OrderStatus.PRODUCING);
        if (producing.isEmpty()) return Optional.empty();

        Order order = producing.get(0);
        Sample sample = sampleRepository.findById(order.getSampleId())
                .orElseThrow(() -> new IllegalArgumentException("시료를 찾을 수 없습니다: " + order.getSampleId()));

        LocalDateTime startedAt = order.getProductionStartedAt();
        LocalDateTime now = LocalDateTime.now(clock);
        long elapsed = startedAt != null ? Duration.between(startedAt, now).toSeconds() : 0;
        int total = order.getTotalProductionSeconds();
        int progressPercent = total > 0 ? (int) Math.min(100, elapsed * 100 / total) : 0;
        LocalDateTime eta = startedAt != null ? startedAt.plusSeconds(total) : now;

        int shortage = Math.max(0, order.getQuantity() - sample.getStock());
        int actualProduction = shortage > 0
                ? calculateActualProduction(shortage, sample.getYield())
                : order.getQuantity();

        return Optional.of(new ProductionProgressInfo(
                order,
                sample.getName(),
                sample.getStock(),
                shortage,
                actualProduction,
                sample.getYield(),
                sample.getAvgProductionTime(),
                elapsed,
                total,
                progressPercent,
                eta
        ));
    }
}
