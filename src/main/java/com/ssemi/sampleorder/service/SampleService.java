package com.ssemi.sampleorder.service;

import com.ssemi.sampleorder.model.Sample;
import com.ssemi.sampleorder.repository.SampleRepository;

import java.util.List;
import java.util.UUID;

public class SampleService {

    private final SampleRepository sampleRepository;

    public SampleService(SampleRepository sampleRepository) {
        this.sampleRepository = sampleRepository;
    }

    public Sample register(String name, int avgProductionTime, double yield, int stock) {
        // Sample 생성자가 유효성 검증 수행 (name blank, avgTime<=0, yield범위, stock<0)
        Sample sample = new Sample(UUID.randomUUID().toString(), name, avgProductionTime, yield, stock);
        sampleRepository.save(sample);
        return sample;
    }

    public List<Sample> listSamples() {
        return sampleRepository.findAll();
    }

    public List<Sample> searchSample(String keyword) {
        if (keyword == null) throw new IllegalArgumentException("검색 키워드는 null일 수 없습니다.");
        return sampleRepository.findByName(keyword);
    }

    public int getStock(String sampleId) {
        if (sampleId == null) throw new IllegalArgumentException("sampleId는 null일 수 없습니다.");
        return sampleRepository.findById(sampleId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 시료입니다: " + sampleId))
                .getStock();
    }

    public Sample getSample(String sampleId) {
        if (sampleId == null) throw new IllegalArgumentException("sampleId는 null일 수 없습니다.");
        return sampleRepository.findById(sampleId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 시료입니다: " + sampleId));
    }
}
