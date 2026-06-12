# Phase 3 테스트 계획 (RED Phase)

## 대상 클래스
- `SampleService` — 시료 등록·조회·검색, 재고 관리
- `OrderService`  — 주문 접수·승인·거절, 재고 분기 처리 ← 핵심 복잡도
- `ProductionService` — 생산라인 큐 관리, 수율 계산 공식 ← 핵심 복잡도
- `MonitorService` — 상태별 주문 집계, 재고 상태 판정

## 설계 방향

- Service는 Repository 인터페이스에만 의존 → 테스트에서 In-Memory 가짜 구현체(Fake) 사용
- Fake Repository는 `HashMap` 기반으로 각 테스트 클래스 내부에 inner class로 선언
- Mock 라이브러리 미사용 (추가 의존성 없음)

---

## 1. SampleServiceTest

### 시료 등록 (Regression)
| # | 테스트명 | 입력 | 기대 결과 | 구분 |
|---|---------|------|---------|------|
| 1 | 유효한 인자로 시료 등록 성공 | `register("GaAs", 60, 0.85, 50)` | Sample 반환, Repository에 저장됨 | Regression |
| 2 | 등록 후 listSamples에 포함됨 | register 후 `listSamples()` | 목록에 포함 | Regression |
| 3 | 등록된 시료 ID는 고유함 | register 2건 | 두 ID가 서로 다름 | Regression |

### 시료 조회·검색 (Regression)
| # | 테스트명 | 입력 | 기대 결과 | 구분 |
|---|---------|------|---------|------|
| 4 | listSamples 전체 목록 반환 | register 3건 후 `listSamples()` | size == 3 | Regression |
| 5 | searchSample 키워드 일치 반환 | `searchSample("GaAs")` | 해당 시료 포함 | Regression |
| 6 | getStock 재고 수량 반환 | register(stock=50) 후 `getStock(id)` | 50 | Regression |

### 유효성 (Safety)
| # | 테스트명 | 입력 | 기대 결과 | 구분 |
|---|---------|------|---------|------|
| 7 | name null 등록 불가 | `register(null, 60, 0.85, 0)` | `IllegalArgumentException` | Safety |
| 8 | yield 범위 초과 등록 불가 | `register("A", 60, 1.5, 0)` | `IllegalArgumentException` | Safety |
| 9 | 존재하지 않는 ID getStock | `getStock("없는ID")` | `IllegalArgumentException` | Safety |
| 10 | searchSample null 키워드 | `searchSample(null)` | `IllegalArgumentException` | Safety |

---

## 2. OrderServiceTest ← 가장 중요

### 주문 접수 (Regression)
| # | 테스트명 | 입력 | 기대 결과 | 구분 |
|---|---------|------|---------|------|
| 1 | 주문 접수 성공 → RESERVED 상태 | `reserveOrder(sampleId, "고객", 10)` | status == RESERVED | Regression |
| 2 | 접수된 주문은 Repository에 저장됨 | reserveOrder 후 `findById` | 주문 존재 | Regression |
| 3 | 접수 주문 ID는 고유함 | reserveOrder 2건 | 두 ID가 다름 | Regression |

### 재고 충분 시 승인 (Regression) ← 핵심 분기
| # | 테스트명 | 입력 | 기대 결과 | 구분 |
|---|---------|------|---------|------|
| 4 | 재고 충분 → CONFIRMED 전환 | stock=50, quantity=10 승인 | status == CONFIRMED | Regression |
| 5 | 재고 충분 → 재고 정확히 차감됨 | stock=50, quantity=10 승인 | stock == 40 | Regression |
| 6 | 재고 == 주문량 (경계값) → CONFIRMED | stock=10, quantity=10 승인 | status == CONFIRMED, stock == 0 | Regression |

### 재고 부족 시 승인 (Regression) ← 핵심 분기
| # | 테스트명 | 입력 | 기대 결과 | 구분 |
|---|---------|------|---------|------|
| 7 | 재고 부족 → PRODUCING 전환 | stock=5, quantity=10 승인 | status == PRODUCING | Regression |
| 8 | 재고 부족 → ProductionQueue에 진입 | stock=5, quantity=10 승인 | queue.size() == 1 | Regression |
| 9 | 재고 == 0 (경계값) → PRODUCING | stock=0, quantity=10 승인 | status == PRODUCING | Regression |
| 10 | 재고 부족 시 재고 차감 없음 | stock=5, quantity=10 승인 | stock 변화 없음 | Regression |

### 주문 거절 (Regression)
| # | 테스트명 | 입력 | 기대 결과 | 구분 |
|---|---------|------|---------|------|
| 11 | 거절 → REJECTED 전환 | `rejectOrder(orderId)` | status == REJECTED | Regression |
| 12 | 거절 시 재고 변화 없음 | stock=50 상태에서 rejectOrder | stock == 50 | Regression |

### 유효성 (Safety)
| # | 테스트명 | 입력 | 기대 결과 | 구분 |
|---|---------|------|---------|------|
| 13 | 존재하지 않는 주문 승인 | `approveOrder("없는ID")` | `IllegalArgumentException` | Safety |
| 14 | 존재하지 않는 주문 거절 | `rejectOrder("없는ID")` | `IllegalArgumentException` | Safety |
| 15 | RESERVED가 아닌 주문 승인 시도 | CONFIRMED 상태 주문 approveOrder | `IllegalStateException` | Safety |
| 16 | RESERVED가 아닌 주문 거절 시도 | CONFIRMED 상태 주문 rejectOrder | `IllegalStateException` | Safety |
| 17 | quantity 0 이하 주문 접수 불가 | `reserveOrder(id, "고객", 0)` | `IllegalArgumentException` | Safety |

---

## 3. ProductionServiceTest ← 수율 계산 핵심

### 수율 계산 공식 검증 (Regression)
공식: `실 생산량 = Math.ceil(부족분 / (수율 * 0.9))`

| # | 테스트명 | 입력 | 기대 결과 | 구분 |
|---|---------|------|---------|------|
| 1 | 기본 수율 계산 | shortage=10, yield=0.9 | `ceil(10/0.81)` = **13** | Regression |
| 2 | ceil 처리 확인 — 나누어 떨어지지 않는 경우 | shortage=1, yield=1.0 | `ceil(1/0.9)` = **2** | Regression |
| 3 | 정확히 나누어 떨어지는 경우 | shortage=9, yield=1.0 | `ceil(9/0.9)` = **10** | Regression |
| 4 | 낮은 수율 케이스 | shortage=10, yield=0.5 | `ceil(10/0.45)` = **23** | Regression |
| 5 | 총 생산시간 = avgTime * 실생산량 | avgTime=60, actualProduction=13 | 780(분) | Regression |

### 생산라인 처리 (Regression)
| # | 테스트명 | 입력 | 기대 결과 | 구분 |
|---|---------|------|---------|------|
| 6 | processNext — 큐에서 꺼내 CONFIRMED 전환 | PRODUCING 주문 1건 큐에 존재 | status == CONFIRMED | Regression |
| 7 | processNext — 재고 반영됨 (생산량만큼 증가 후 주문량 차감) | stock=5, quantity=10 → shortage=5 | 처리 후 stock == 0, status CONFIRMED | Regression |
| 8 | processNext 후 큐 size 감소 | 큐 2건 → processNext | queue.size() == 1 | Regression |
| 9 | getQueueSnapshot — 현재 대기 목록 반환 | 큐 3건 | size == 3 | Regression |

### 유효성 (Safety)
| # | 테스트명 | 입력 | 기대 결과 | 구분 |
|---|---------|------|---------|------|
| 10 | 빈 큐에서 processNext | 빈 큐 | `IllegalStateException` | Safety |
| 11 | shortage 0 이하 계산 불가 | `calculateActualProduction(0, 0.9)` | `IllegalArgumentException` | Safety |
| 12 | yield 범위 외 계산 불가 | `calculateActualProduction(10, 0.0)` | `IllegalArgumentException` | Safety |

---

## 4. MonitorServiceTest

### 주문 집계 (Regression)
| # | 테스트명 | 입력 | 기대 결과 | 구분 |
|---|---------|------|---------|------|
| 1 | 상태별 주문 수 집계 정확성 | RESERVED 2, CONFIRMED 1, PRODUCING 1 저장 | 각 카운트 일치 | Regression |
| 2 | REJECTED 주문은 집계에서 제외 | RESERVED 1, REJECTED 2 저장 | REJECTED count == 0 (모니터링 제외) | Regression |
| 3 | getOrdersByStatus — 해당 상태 목록 반환 | RESERVED 2건 | size == 2, 모두 RESERVED | Regression |
| 4 | 주문 없을 때 집계 — 모든 count 0 | 빈 Repository | 모든 상태 count == 0 | Regression |

### 재고 현황 (Regression)
| # | 테스트명 | 입력 | 기대 결과 | 구분 |
|---|---------|------|---------|------|
| 5 | stock==0 → DEPLETED 표기 | stock=0인 시료 | StockStatus.DEPLETED | Regression |
| 6 | stock < 총 주문량 → SHORTAGE 표기 | stock=5, 주문합계=10 | StockStatus.SHORTAGE | Regression |
| 7 | stock >= 총 주문량 → SUFFICIENT 표기 | stock=20, 주문합계=10 | StockStatus.SUFFICIENT | Regression |

### 유효성 (Safety)
| # | 테스트명 | 입력 | 기대 결과 | 구분 |
|---|---------|------|---------|------|
| 8 | getOrdersByStatus(null) | null 전달 | `IllegalArgumentException` | Safety |

---

## 테스트 파일 위치

```
src/test/java/com/ssemi/sampleorder/
└── service/
    ├── SampleServiceTest.java      (10개)
    ├── OrderServiceTest.java       (17개)
    ├── ProductionServiceTest.java  (12개)
    └── MonitorServiceTest.java     (8개)
```

**총 47개 테스트 케이스** — Safety 13개 / Regression 34개

---

## 생성될 프로덕션 클래스 (GREEN Phase 대상)

```
src/main/java/com/ssemi/sampleorder/service/
├── SampleService.java
├── OrderService.java
├── ProductionService.java
└── MonitorService.java
```
