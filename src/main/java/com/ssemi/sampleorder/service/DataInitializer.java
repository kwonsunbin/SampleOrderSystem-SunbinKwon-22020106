package com.ssemi.sampleorder.service;

import com.ssemi.sampleorder.model.Sample;
import com.ssemi.sampleorder.repository.OrderRepository;
import com.ssemi.sampleorder.repository.SampleRepository;

import java.util.List;

public class DataInitializer {

    private static final List<Sample> SEED_SAMPLES = List.of(
        new Sample("S001", "실리콘 웨이퍼 8인치",   3, 0.92,  1),
        new Sample("S002", "실리콘 웨이퍼 12인치",  3, 0.88,  6),
        new Sample("S003", "GaN 에피택시 웨이퍼",   4, 0.75, 15),
        new Sample("S004", "SiC 파워 소자 시료",    3, 0.80, 20),
        new Sample("S005", "MEMS 압력센서 시료",    2, 0.95,  9)
    );

    private final SampleRepository sampleRepository;
    private final OrderRepository  orderRepository;

    public DataInitializer(SampleRepository sampleRepository, OrderRepository orderRepository) {
        this.sampleRepository = sampleRepository;
        this.orderRepository  = orderRepository;
    }

    public void seedSamples() {
        if (!sampleRepository.findAll().isEmpty()) return;
        SEED_SAMPLES.forEach(sampleRepository::save);
    }

    public void clearOrders() {
        orderRepository.deleteAll();
    }
}
