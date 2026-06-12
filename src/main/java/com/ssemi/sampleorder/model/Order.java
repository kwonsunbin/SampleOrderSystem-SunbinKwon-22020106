package com.ssemi.sampleorder.model;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Map;

public class Order {

    private static final Map<OrderStatus, EnumSet<OrderStatus>> ALLOWED_TRANSITIONS = Map.of(
            OrderStatus.RESERVED,  EnumSet.of(OrderStatus.CONFIRMED, OrderStatus.PRODUCING, OrderStatus.REJECTED),
            OrderStatus.PRODUCING, EnumSet.of(OrderStatus.CONFIRMED),
            OrderStatus.CONFIRMED, EnumSet.of(OrderStatus.RELEASED),
            OrderStatus.REJECTED,  EnumSet.noneOf(OrderStatus.class),
            OrderStatus.RELEASED,  EnumSet.noneOf(OrderStatus.class)
    );

    private final String id;
    private final String sampleId;
    private final String customerName;
    private final int quantity;
    private final LocalDateTime createdAt;
    private OrderStatus status;

    private LocalDateTime productionStartedAt;
    private int totalProductionSeconds;

    public Order(String id, String sampleId, String customerName, int quantity) {
        if (customerName == null || customerName.isBlank())
            throw new IllegalArgumentException("customerName은 null이거나 공백일 수 없습니다.");
        if (quantity <= 0)
            throw new IllegalArgumentException("quantity는 1 이상이어야 합니다: " + quantity);

        this.id = id;
        this.sampleId = sampleId;
        this.customerName = customerName;
        this.quantity = quantity;
        this.status = OrderStatus.RESERVED;
        this.createdAt = LocalDateTime.now();
    }

    // JSON 역직렬화 전용 — status, createdAt, 생산 시간 필드를 직접 복원
    public Order(String id, String sampleId, String customerName, int quantity,
                 OrderStatus status, LocalDateTime createdAt,
                 LocalDateTime productionStartedAt, int totalProductionSeconds) {
        if (customerName == null || customerName.isBlank())
            throw new IllegalArgumentException("customerName은 null이거나 공백일 수 없습니다.");
        if (quantity <= 0)
            throw new IllegalArgumentException("quantity는 1 이상이어야 합니다: " + quantity);

        this.id = id;
        this.sampleId = sampleId;
        this.customerName = customerName;
        this.quantity = quantity;
        this.status = status;
        this.createdAt = createdAt;
        this.productionStartedAt = productionStartedAt;
        this.totalProductionSeconds = totalProductionSeconds;
    }

    // 하위 호환 — 기존 JSON에 productionStartedAt 없는 경우
    public Order(String id, String sampleId, String customerName, int quantity,
                 OrderStatus status, LocalDateTime createdAt) {
        this(id, sampleId, customerName, quantity, status, createdAt, null, 0);
    }

    public void transitionTo(OrderStatus next) {
        EnumSet<OrderStatus> allowed = ALLOWED_TRANSITIONS.get(this.status);
        if (!allowed.contains(next)) {
            throw new IllegalStateException(
                    String.format("'%s' 상태에서 '%s' 상태로 전이할 수 없습니다.", this.status, next));
        }
        this.status = next;
    }

    public void startProduction(LocalDateTime startedAt, int totalSeconds) {
        if (this.status != OrderStatus.PRODUCING)
            throw new IllegalStateException("PRODUCING 상태에서만 생산을 시작할 수 있습니다. 현재: " + this.status);
        this.productionStartedAt = startedAt;
        this.totalProductionSeconds = totalSeconds;
    }

    public String getId() { return id; }
    public String getSampleId() { return sampleId; }
    public String getCustomerName() { return customerName; }
    public int getQuantity() { return quantity; }
    public OrderStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getProductionStartedAt() { return productionStartedAt; }
    public int getTotalProductionSeconds() { return totalProductionSeconds; }
}
