# Phase 3 테스트 계획 (RED Phase)

## 대상 클래스
- `SampleService` — 시료 등록·조회·검색, 재고 관리
- `OrderService`  — 주문 접수·승인·거절, 재고 분기 처리 ← 핵심 복잡도
- `ProductionService` — 생산라인 큐 관리, 수율 계산 공식 ← 핵심 복잡도
- `MonitorService` — 상태별 주문 집계, 재고 상태 판정

## 설계 방향

- Service는 Repository 인터페이스에만 의존 → 테스트에서 In-Memory Fake 구현체 사용
- Fake Repository는 `HashMap` 기반 inner class로 각 테스트 클래스 내부에 선언
- Mock 라이브러리 미사용 (추가 의존성 없음)

---

## 1. SampleServiceTest

### 시료 등록 (Regression)
| # | 테스트명 | 기대 결과 | 구분 |
|---|---------|---------|------|
| 1 | 유효한 인자로 시료 등록 성공, 반환된 Sample 필드 일치 | id/name/avgTime/yield/stock 일치 | Regression |
| 2 | 등록 후 listSamples에 포함됨 | 목록에 포함 | Regression |
| 3 | 등록된 시료 ID는 고유함 (2건 등록) | 두 ID가 서로 다름 | Regression |

### 시료 조회·검색 (Regression)
| # | 테스트명 | 기대 결과 | 구분 |
|---|---------|---------|------|
| 4 | listSamples 전체 목록 반환 | 등록 3건 → size == 3 | Regression |
| 5 | listSamples 빈 상태 → 빈 목록 반환 | 빈 List, 예외 없음 | Regression |
| 6 | searchSample 키워드 일치 반환 | 해당 시료 포함 | Regression |
| 7 | searchSample 미일치 → 빈 목록 반환 | 빈 List | Regression |
| 8 | getStock 재고 수량 정확 반환 | stock=50 등록 후 getStock → 50 | Regression |

### 유효성 (Safety)
| # | 테스트명 | 기대 결과 | 구분 |
|---|---------|---------|------|
| 9 | name null 등록 불가 | `IllegalArgumentException` | Safety |
| 10 | name blank 등록 불가 | `IllegalArgumentException` | Safety |
| 11 | avgProductionTime ≤ 0 등록 불가 | `IllegalArgumentException` | Safety |
| 12 | stock 음수 등록 불가 | `IllegalArgumentException` | Safety |
| 13 | yield 범위 초과 등록 불가 (yield=1.5) | `IllegalArgumentException` | Safety |
| 14 | getStock(null) → 예외 | `IllegalArgumentException` | Safety |
| 15 | 존재하지 않는 ID getStock → 예외 | `IllegalArgumentException` | Safety |
| 16 | searchSample(null) → 예외 | `IllegalArgumentException` | Safety |

---

## 2. OrderServiceTest ← 가장 중요

### 주문 접수 (Regression)
| # | 테스트명 | 기대 결과 | 구분 |
|---|---------|---------|------|
| 1 | 주문 접수 성공 → status == RESERVED | RESERVED | Regression |
| 2 | 접수된 주문은 Repository에 저장됨 | findById 존재 | Regression |
| 3 | 접수 주문 ID는 고유함 (2건) | 두 ID가 다름 | Regression |
| 4 | listReservedOrders → RESERVED 상태 주문만 반환 | RESERVED 2건, CONFIRMED 1건 저장 시 size==2 | Regression |

### 재고 충분 시 승인 (Regression) ← 핵심 분기
| # | 테스트명 | 기대 결과 | 구분 |
|---|---------|---------|------|
| 5 | 재고 충분 → status == CONFIRMED | stock=50, qty=10 승인 | Regression |
| 6 | 재고 충분 → 재고 정확히 차감 | stock=50, qty=10 → stock==40 | Regression |
| 7 | 재고 == 주문량 경계값 → CONFIRMED + stock==0 | stock=10, qty=10 승인 | Regression |
| 8 | 첫 승인 후 재고 소진 → 두 번째 동일 시료 승인은 PRODUCING | stock=10, qty=10 두 건 순차 승인 | Regression |

### 재고 부족 시 승인 (Regression) ← 핵심 분기
| # | 테스트명 | 기대 결과 | 구분 |
|---|---------|---------|------|
| 9 | 재고 부족 → status == PRODUCING | stock=5, qty=10 승인 | Regression |
| 10 | 재고 부족 → ProductionQueue 진입 | queue.size() == 1 | Regression |
| 11 | 재고 == 0 경계값 → PRODUCING | stock=0, qty=10 승인 | Regression |
| 12 | 재고 부족 시 재고 차감 없음 | stock 변화 없음 | Regression |

### 주문 거절 (Regression)
| # | 테스트명 | 기대 결과 | 구분 |
|---|---------|---------|------|
| 13 | 거절 → status == REJECTED | REJECTED | Regression |
| 14 | 거절 시 재고 변화 없음 | stock 동일 | Regression |

### 유효성 (Safety)
| # | 테스트명 | 기대 결과 | 구분 |
|---|---------|---------|------|
| 15 | reserveOrder null customerName | `IllegalArgumentException` | Safety |
| 16 | reserveOrder quantity ≤ 0 | `IllegalArgumentException` | Safety |
| 17 | approveOrder 존재하지 않는 ID | `IllegalArgumentException` | Safety |
| 18 | approveOrder(null) | `IllegalArgumentException` | Safety |
| 19 | rejectOrder 존재하지 않는 ID | `IllegalArgumentException` | Safety |
| 20 | rejectOrder(null) | `IllegalArgumentException` | Safety |
| 21 | RESERVED가 아닌 주문 승인 시도 | `IllegalStateException` | Safety |
| 22 | RESERVED가 아닌 주문 거절 시도 | `IllegalStateException` | Safety |

---

## 3. ProductionServiceTest ← 수율 계산 핵심

### 수율 계산 공식 (Regression)
공식: `실 생산량 = Math.ceil(부족분 / (수율 * 0.9))`

| # | 테스트명 | 입력 | 기대값 | 구분 |
|---|---------|------|-------|------|
| 1 | 기본 케이스 | shortage=10, yield=0.9 | `ceil(10/0.81)` = **13** | Regression |
| 2 | ceil 처리 — 나누어 떨어지지 않음 | shortage=1, yield=1.0 | `ceil(1/0.9)` = **2** | Regression |
| 3 | 정확히 나누어 떨어짐 | shortage=9, yield=1.0 | `ceil(9/0.9)` = **10** | Regression |
| 4 | 낮은 수율 | shortage=10, yield=0.5 | `ceil(10/0.45)` = **23** | Regression |
| 5 | 총 생산시간 정확성 | avgTime=60, actualProduction=13 | **780**분 | Regression |
| 6 | 총 생산시간 — 생산량 1인 경우 | avgTime=120, actualProduction=1 | **120**분 | Regression |

### 생산라인 처리 (Regression)
| # | 테스트명 | 기대 결과 | 구분 |
|---|---------|---------|------|
| 7 | processNext → CONFIRMED 전환 | status == CONFIRMED | Regression |
| 8 | processNext → 재고 반영 (생산 후 주문량 차감) | stock=5, qty=10, shortage=5 → 처리 후 stock==0 | Regression |
| 9 | processNext 후 queue size 감소 | 2건 → processNext → size==1 | Regression |
| 10 | FIFO 보장 — 먼저 접수된 주문이 먼저 처리됨 | 2건 enqueue → processNext → 첫 번째 주문 처리 | Regression |
| 11 | getQueueSnapshot → 현재 대기 목록 반환 | 3건 enqueue → size==3 | Regression |

### 유효성 (Safety)
| # | 테스트명 | 기대 결과 | 구분 |
|---|---------|---------|------|
| 12 | 빈 큐에서 processNext | `IllegalStateException` | Safety |
| 13 | shortage ≤ 0 계산 불가 | `IllegalArgumentException` | Safety |
| 14 | shortage 음수 계산 불가 | `IllegalArgumentException` | Safety |
| 15 | yield 범위 외 계산 불가 (yield=0.0) | `IllegalArgumentException` | Safety |

---

## 4. MonitorServiceTest

### 주문 집계 (Regression)
| # | 테스트명 | 기대 결과 | 구분 |
|---|---------|---------|------|
| 1 | 상태별 주문 수 집계 정확성 | RESERVED 2, CONFIRMED 1, PRODUCING 1 → 각 카운트 일치 | Regression |
| 2 | REJECTED 주문은 집계에서 제외 | RESERVED 1, REJECTED 2 → REJECTED count == 0 | Regression |
| 3 | RELEASED 주문은 집계에 포함 | RELEASED 2건 → count == 2 | Regression |
| 4 | getOrdersByStatus → 해당 상태 목록 반환 | RESERVED 2건 저장 → size==2, 모두 RESERVED | Regression |
| 5 | 주문 없을 때 모든 count == 0 | 빈 Repository | 모두 0 | Regression |

### 재고 현황 (Regression)
| # | 테스트명 | 기대 결과 | 구분 |
|---|---------|---------|------|
| 6 | stock==0 → DEPLETED | StockStatus.DEPLETED | Regression |
| 7 | stock < 총 주문량 → SHORTAGE | stock=5, 주문합계=10 | Regression |
| 8 | stock >= 총 주문량 → SUFFICIENT | stock=20, 주문합계=10 | Regression |
| 9 | 다중 시료 각각 StockStatus 정확 판정 | 시료3개, 각각 DEPLETED/SHORTAGE/SUFFICIENT | Regression |

### 유효성 (Safety)
| # | 테스트명 | 기대 결과 | 구분 |
|---|---------|---------|------|
| 10 | getOrdersByStatus(null) → 예외 | `IllegalArgumentException` | Safety |
| 11 | getOrdersByStatus(REJECTED) → 예외 (모니터링 대상 외) | `IllegalArgumentException` | Safety |

---

## 테스트 파일 위치

```
src/test/java/com/ssemi/sampleorder/
└── service/
    ├── SampleServiceTest.java      (16개)
    ├── OrderServiceTest.java       (22개)
    ├── ProductionServiceTest.java  (15개)
    └── MonitorServiceTest.java     (11개)
```

**총 64개 테스트 케이스** — Safety 20개 / Regression 44개

---

## 생성될 프로덕션 클래스 (GREEN Phase 대상)

```
src/main/java/com/ssemi/sampleorder/service/
├── SampleService.java
├── OrderService.java
├── ProductionService.java
└── MonitorService.java
```
