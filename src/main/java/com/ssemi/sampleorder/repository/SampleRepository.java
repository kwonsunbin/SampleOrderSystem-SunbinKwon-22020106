package com.ssemi.sampleorder.repository;

import com.ssemi.sampleorder.model.Sample;

import java.util.List;
import java.util.Optional;

public interface SampleRepository {
    void save(Sample sample);
    Optional<Sample> findById(String id);
    List<Sample> findAll();
    List<Sample> findByName(String keyword);
    void delete(String id);
}
