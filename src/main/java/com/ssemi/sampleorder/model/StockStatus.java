package com.ssemi.sampleorder.model;

public enum StockStatus {
    SUFFICIENT, // 여유
    SHORTAGE,   // 부족
    DEPLETED;   // 고갈

    public static StockStatus of(int stock, int demand) {
        if (stock < 0) throw new IllegalArgumentException("stock은 0 이상이어야 합니다: " + stock);
        if (demand < 0) throw new IllegalArgumentException("demand는 0 이상이어야 합니다: " + demand);

        if (demand == 0) return SUFFICIENT;
        if (stock == 0)  return DEPLETED;
        if (stock < demand) return SHORTAGE;
        return SUFFICIENT;
    }
}
