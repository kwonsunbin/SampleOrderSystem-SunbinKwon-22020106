package com.ssemi.sampleorder.model;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.NoSuchElementException;

public class ProductionQueue {

    private final Deque<Order> queue = new ArrayDeque<>();

    public void enqueue(Order order) {
        if (order == null)
            throw new IllegalArgumentException("null 주문은 큐에 추가할 수 없습니다.");
        queue.addLast(order);
    }

    public Order dequeue() {
        if (queue.isEmpty())
            throw new NoSuchElementException("생산 큐가 비어 있습니다.");
        return queue.pollFirst();
    }

    public Order peek() {
        if (queue.isEmpty())
            throw new NoSuchElementException("생산 큐가 비어 있습니다.");
        return queue.peekFirst();
    }

    public List<Order> toList() { return new ArrayList<>(queue); }
    public boolean isEmpty() { return queue.isEmpty(); }
    public int size() { return queue.size(); }
}
