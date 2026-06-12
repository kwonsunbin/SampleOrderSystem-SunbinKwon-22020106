package com.ssemi.sampleorder.model;

public class Sample {

    private final String id;
    private final String name;
    private final int avgProductionTime; // 초 단위
    private final double yield;          // 0 초과 ~ 1.0 이하
    private int stock;

    public Sample(String id, String name, int avgProductionTime, double yield, int stock) {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("name은 null이거나 공백일 수 없습니다.");
        if (avgProductionTime <= 0)
            throw new IllegalArgumentException("avgProductionTime은 1 이상이어야 합니다: " + avgProductionTime);
        if (yield <= 0.0 || yield > 1.0)
            throw new IllegalArgumentException("yield는 0 초과 1 이하여야 합니다: " + yield);
        if (stock < 0)
            throw new IllegalArgumentException("stock은 0 이상이어야 합니다: " + stock);

        this.id = id;
        this.name = name;
        this.avgProductionTime = avgProductionTime;
        this.yield = yield;
        this.stock = stock;
    }

    public void addStock(int amount) {
        if (amount < 0)
            throw new IllegalArgumentException("추가 수량은 0 이상이어야 합니다: " + amount);
        this.stock += amount;
    }

    public void reduceStock(int amount) {
        if (amount < 0)
            throw new IllegalArgumentException("차감 수량은 0 이상이어야 합니다: " + amount);
        if (amount > this.stock)
            throw new IllegalArgumentException("재고보다 많은 수량을 차감할 수 없습니다. 현재 재고: " + stock + ", 요청: " + amount);
        this.stock -= amount;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public int getAvgProductionTime() { return avgProductionTime; }
    public double getYield() { return yield; }
    public int getStock() { return stock; }
}
