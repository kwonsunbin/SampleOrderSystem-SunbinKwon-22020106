package com.ssemi.sampleorder.service;

import com.ssemi.sampleorder.model.StockStatus;

public class SampleStockInfo {

    private final String sampleId;
    private final String sampleName;
    private final int stock;
    private final int demand;
    private final StockStatus stockStatus;

    public SampleStockInfo(String sampleId, String sampleName, int stock, int demand, StockStatus stockStatus) {
        this.sampleId    = sampleId;
        this.sampleName  = sampleName;
        this.stock       = stock;
        this.demand      = demand;
        this.stockStatus = stockStatus;
    }

    public String getSampleId()       { return sampleId; }
    public String getSampleName()     { return sampleName; }
    public int getStock()             { return stock; }
    public int getDemand()            { return demand; }
    public StockStatus getStockStatus() { return stockStatus; }
}
