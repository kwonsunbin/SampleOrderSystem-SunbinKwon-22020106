# Phase 1 테스트 계획 (RED Phase)

## 대상 클래스
- `OrderStatus` (Enum)
- `StockStatus` (Enum)
- `Sample`
- `Order`
- `ProductionQueue`

---

## 1. OrderTest

### Happy Path
| # | 테스트명 | 입력 | 기대 결과 | 구분 |
|---|---------|------|---------|------|
| 1 | 초기 상태는 RESERVED | `new Order(...)` | `status == RESERVED` | Regression |
| 2 | RESERVED → CONFIRMED 전이 성공 | `transitionTo(CONFIRMED)` | 상태 변경됨 | Regression |
| 3 | RESERVED → PRODUCING 전이 성공 | `transitionTo(PRODUCING)` | 상태 변경됨 | Regression |
| 4 | RESERVED → REJECTED 전이 성공 | `transitionTo(REJECTED)` | 상태 변경됨 | Regression |
| 5 | PRODUCING → CONFIRMED 전이 성공 | `transitionTo(CONFIRMED)` | 상태 변경됨 | Regression |
| 6 | CONFIRMED → RELEASED 전이 성공 | `transitionTo(RELEASED)` | 상태 변경됨 | Regression |
| 7 | 전체 흐름 연속 전이: RESERVED → PRODUCING → CONFIRMED → RELEASED | 3회 연속 `transitionTo` | 각 단계 상태 정확히 전이됨 | Regression |

### 불허 전이 (Safety)
| # | 테스트명 | 입력 | 기대 결과 | 구분 |
|---|---------|------|---------|------|
| 8 | CONFIRMED → PRODUCING 전이 불가 | `transitionTo(PRODUCING)` | `IllegalStateException` | Safety |
| 9 | RELEASED → CONFIRMED 전이 불가 | `transitionTo(CONFIRMED)` | `IllegalStateException` | Safety |
| 10 | REJECTED → CONFIRMED 전이 불가 | `transitionTo(CONFIRMED)` | `IllegalStateException` | Safety |
| 11 | RELEASED → RESERVED 전이 불가 | `transitionTo(RESERVED)` | `IllegalStateException` | Safety |
| 12 | RELEASED 이후 어떤 전이도 불가 | `transitionTo(임의 상태)` | `IllegalStateException` | Safety |

### 생성 유효성 (Safety)
| # | 테스트명 | 입력 | 기대 결과 | 구분 |
|---|---------|------|---------|------|
| 13 | customerName null 불가 | `customerName = null` | `IllegalArgumentException` | Safety |
| 14 | customerName blank 불가 | `customerName = "  "` | `IllegalArgumentException` | Safety |
| 15 | quantity ≤ 0 불가 | `quantity = 0` | `IllegalArgumentException` | Safety |
| 16 | quantity 음수 불가 | `quantity = -1` | `IllegalArgumentException` | Safety |

### 불변성 (Regression)
| # | 테스트명 | 입력 | 기대 결과 | 구분 |
|---|---------|------|---------|------|
| 17 | 상태 전이 후 id 변경 없음 | `transitionTo(CONFIRMED)` | `id` 동일 | Regression |
| 18 | 상태 전이 후 quantity 변경 없음 | `transitionTo(CONFIRMED)` | `quantity` 동일 | Regression |
| 19 | 상태 전이 후 customerName 변경 없음 | `transitionTo(CONFIRMED)` | `customerName` 동일 | Regression |

---

## 2. ProductionQueueTest

### Happy Path
| # | 테스트명 | 입력 | 기대 결과 | 구분 |
|---|---------|------|---------|------|
| 1 | 큐 초기 상태는 비어 있음 | `new ProductionQueue()` | `isEmpty() == true`, `size() == 0` | Regression |
| 2 | enqueue 후 isEmpty false | `enqueue(order)` | `isEmpty() == false`, `size() == 1` | Regression |
| 3 | FIFO 순서 보장 — 먼저 넣은 것이 먼저 나옴 | 순서대로 enqueue 3건 | dequeue 순서 == enqueue 순서 | Regression |
| 4 | dequeue 후 size 감소 | enqueue 2건 후 dequeue | `size() == 1` | Regression |
| 5 | peek은 꺼내지 않고 조회 | enqueue 후 `peek()` | 동일 주문 반환, `size()` 변화 없음 | Regression |
| 6 | 반복 enqueue/dequeue 후 size 일관성 | 5건 enqueue → 3건 dequeue → 2건 enqueue | `size() == 4` | Regression |

### 경계값 / 예외 (Safety)
| # | 테스트명 | 입력 | 기대 결과 | 구분 |
|---|---------|------|---------|------|
| 7 | 빈 큐에서 dequeue 시 예외 | 빈 큐에서 `dequeue()` | `NoSuchElementException` | Safety |
| 8 | 빈 큐에서 peek 시 예외 | 빈 큐에서 `peek()` | `NoSuchElementException` | Safety |
| 9 | null 주문 enqueue 불가 | `enqueue(null)` | `IllegalArgumentException` | Safety |

---

## 3. SampleTest

### Happy Path
| # | 테스트명 | 입력 | 기대 결과 | 구분 |
|---|---------|------|---------|------|
| 1 | 정상 생성 | 유효한 id, name, avgTime=60, yield=0.85, stock=50 | 객체 생성 성공, 필드 일치 | Regression |
| 2 | addStock 후 재고 증가 | `stock=10, addStock(5)` | `stock == 15` | Regression |
| 3 | reduceStock 후 재고 감소 | `stock=10, reduceStock(3)` | `stock == 7` | Regression |
| 4 | addStock 후 reduceStock 연속 연산 일관성 | `stock=10, addStock(5), reduceStock(8)` | `stock == 7` | Regression |
| 5 | reduceStock으로 재고 정확히 0 가능 | `stock=5, reduceStock(5)` | `stock == 0` | Regression |

### 생성 유효성 (Safety)
| # | 테스트명 | 입력 | 기대 결과 | 구분 |
|---|---------|------|---------|------|
| 6 | yield 0.0 이하 불가 | `yield = 0.0` | `IllegalArgumentException` | Safety |
| 7 | yield 1.0 초과 불가 | `yield = 1.1` | `IllegalArgumentException` | Safety |
| 8 | stock 음수 불가 | `stock = -1` | `IllegalArgumentException` | Safety |
| 9 | avgProductionTime ≤ 0 불가 | `avgProductionTime = 0` | `IllegalArgumentException` | Safety |
| 10 | name null 불가 | `name = null` | `IllegalArgumentException` | Safety |
| 11 | name blank 불가 | `name = ""` | `IllegalArgumentException` | Safety |

### 연산 유효성 (Safety)
| # | 테스트명 | 입력 | 기대 결과 | 구분 |
|---|---------|------|---------|------|
| 12 | reduceStock 초과 시 예외 | `stock=5, reduceStock(10)` | `IllegalArgumentException` | Safety |
| 13 | addStock 음수 불가 | `addStock(-1)` | `IllegalArgumentException` | Safety |
| 14 | reduceStock 음수 불가 | `reduceStock(-1)` | `IllegalArgumentException` | Safety |

---

## 4. StockStatusTest

### 판정 로직 (Regression)
| # | 테스트명 | 입력 | 기대 결과 | 구분 |
|---|---------|------|---------|------|
| 1 | stock==0 → DEPLETED | `stock=0, demand=5` | `DEPLETED` | Regression |
| 2 | 0 < stock < demand → SHORTAGE | `stock=3, demand=5` | `SHORTAGE` | Regression |
| 3 | stock >= demand → SUFFICIENT | `stock=5, demand=5` | `SUFFICIENT` | Regression |
| 4 | stock > demand → SUFFICIENT | `stock=10, demand=5` | `SUFFICIENT` | Regression |
| 5 | demand==0, stock==0 → SUFFICIENT (주문 없음) | `stock=0, demand=0` | `SUFFICIENT` | Regression |
| 6 | demand==0, stock>0 → SUFFICIENT | `stock=5, demand=0` | `SUFFICIENT` | Regression |

### 경계값 (Safety)
| # | 테스트명 | 입력 | 기대 결과 | 구분 |
|---|---------|------|---------|------|
| 7 | stock 음수 불가 | `stock=-1, demand=5` | `IllegalArgumentException` | Safety |
| 8 | demand 음수 불가 | `stock=5, demand=-1` | `IllegalArgumentException` | Safety |

---

## 테스트 파일 위치

```
src/test/java/com/ssemi/sampleorder/
└── model/
    ├── OrderTest.java              (19개 케이스)
    ├── ProductionQueueTest.java    (9개 케이스)
    ├── SampleTest.java             (14개 케이스)
    └── StockStatusTest.java        (8개 케이스)
```

**총 50개 테스트 케이스** — Safety: 22개 / Regression: 28개

---

## 생성될 프로덕션 클래스 (GREEN Phase 대상)

```
src/main/java/com/ssemi/sampleorder/model/
├── OrderStatus.java    — Enum: RESERVED, REJECTED, PRODUCING, CONFIRMED, RELEASED
├── StockStatus.java    — Enum: SUFFICIENT, SHORTAGE, DEPLETED + of(stock, demand) 정적 메서드
├── Sample.java         — 시료 엔티티 (id, name, avgProductionTime, yield, stock)
├── Order.java          — 주문 엔티티 (id, sampleId, customerName, quantity, status, createdAt)
└── ProductionQueue.java — FIFO 큐 (ArrayDeque<Order> 래핑)
```
