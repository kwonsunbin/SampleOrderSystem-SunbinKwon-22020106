package com.ssemi.sampleorder.service;

import com.ssemi.sampleorder.model.Order;

import java.time.LocalDateTime;

public class ProductionProgressInfo {

    private final Order order;
    private final String sampleName;
    private final int currentStock;
    private final int shortage;
    private final int actualProduction;
    private final double yield;
    private final int avgProductionTime;
    private final long elapsedSeconds;
    private final int totalSeconds;
    private final int progressPercent;
    private final LocalDateTime estimatedCompletionTime;

    public ProductionProgressInfo(Order order, String sampleName,
                                  int currentStock, int shortage, int actualProduction,
                                  double yield, int avgProductionTime,
                                  long elapsedSeconds, int totalSeconds,
                                  int progressPercent, LocalDateTime estimatedCompletionTime) {
        this.order = order;
        this.sampleName = sampleName;
        this.currentStock = currentStock;
        this.shortage = shortage;
        this.actualProduction = actualProduction;
        this.yield = yield;
        this.avgProductionTime = avgProductionTime;
        this.elapsedSeconds = elapsedSeconds;
        this.totalSeconds = totalSeconds;
        this.progressPercent = progressPercent;
        this.estimatedCompletionTime = estimatedCompletionTime;
    }

    public Order getOrder() { return order; }
    public String getSampleName() { return sampleName; }
    public int getCurrentStock() { return currentStock; }
    public int getShortage() { return shortage; }
    public int getActualProduction() { return actualProduction; }
    public double getYield() { return yield; }
    public int getAvgProductionTime() { return avgProductionTime; }
    public long getElapsedSeconds() { return elapsedSeconds; }
    public int getTotalSeconds() { return totalSeconds; }
    public int getProgressPercent() { return progressPercent; }
    public LocalDateTime getEstimatedCompletionTime() { return estimatedCompletionTime; }
}
