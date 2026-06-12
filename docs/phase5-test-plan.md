# Phase 5 테스트 계획 (RED Phase)

## 목표
전체 시나리오 통합 검증 + Main.java(Composition Root) 구현

---

## 설계 방향

- **통합 테스트 방식:** Mock 없이 실제 Service + Repository 조합으로 전체 흐름 검증
- **파일 경로:** 각 테스트마다 `data/test/integration/` 임시 디렉터리 사용, `@AfterEach`에서 삭제
- **대상 클래스:** `IntegrationTest` (단일 테스트 클래스, 시나리오별 `@Nested`)
- **ProductionQueue:** 각 테스트마다 새 인스턴스 생성 (상태 격리)

---

## 시나리오 테스트 (IntegrationTest)

### Scenario 1: 재고 부족 → PRODUCING 진입
**흐름:** 시료 등록(재고=0) → 주문 접수 → 승인 → PRODUCING + 생산 큐 진입

| # | 검증 항목 | 기대 결과 | 구분 |
|---|---------|---------|------|
| 1 | 재고=0 시료에 대해 approveOrder() 호출 | order.getStatus() == PRODUCING | Regression |
| 2 | 승인 후 productionQueue에 해당 주문이 존재 | queue.size() == 1 | Regression |
| 3 | 승인 후 시료 재고는 변동 없음 (차감 안 됨) | stock == 0 | Regression |

### Scenario 2: 생산라인 실행 → 출고 → RELEASED
**흐름:** (Scenario 1 이어서) processNext() → CONFIRMED → releaseOrder() → RELEASED

| # | 검증 항목 | 기대 결과 | 구분 |
|---|---------|---------|------|
| 4 | processNext() 호출 후 주문 상태 | order.getStatus() == CONFIRMED | Regression |
| 5 | processNext() 후 시료 재고 반영 (생산량 추가 후 주문량 차감) | stock >= 0 | Regression |
| 6 | releaseOrder() 호출 후 주문 상태 | order.getStatus() == RELEASED | Regression |
| 7 | RELEASED 주문은 Repository에 영속됨 | findById().getStatus() == RELEASED | Regression |

### Scenario 3: 재고 충분 → 즉시 CONFIRMED
**흐름:** 시료 등록(재고=100) → 주문 접수(수량=10) → 승인 → 즉시 CONFIRMED + 재고 차감

| # | 검증 항목 | 기대 결과 | 구분 |
|---|---------|---------|------|
| 8  | approveOrder() 결과 상태 | order.getStatus() == CONFIRMED | Regression |
| 9  | 승인 후 시료 재고 차감 | stock == 90 | Regression |
| 10 | 생산 큐에 해당 주문 없음 | queue.isEmpty() == true | Regression |

### Scenario 4: 주문 거절 → 모니터링 미표시
**흐름:** 주문 접수 → 거절 → getOrderCountByStatus() 에서 REJECTED 제외 확인

| # | 검증 항목 | 기대 결과 | 구분 |
|---|---------|---------|------|
| 11 | rejectOrder() 후 주문 상태 | order.getStatus() == REJECTED | Regression |
| 12 | getOrderCountByStatus() 반환 map에 REJECTED 키 없음 | !counts.containsKey(REJECTED) | Regression |
| 13 | REJECTED 주문은 모니터링 집계에서 카운트 0 | 총합 == 0 (REJECTED만 있을 때) | Regression |

### Scenario 6: 수율 계산 하드 케이스 — Overflow & Deadlock

#### 배경
- 실 생산량 공식: `(int) Math.ceil(shortage / (yield * 0.9))`
- `yield=0.01, shortage=20_000_000` → 수학적 결과 2,222,222,223 → int 캐스트 시 **-2,072,745,073** (음수 오버플로우)
- 음수가 `addStock()` 으로 전달 → `IllegalArgumentException` → `processNext()` 중단 → 주문 PRODUCING 유지 = **데드락**

#### Overflow 검증
| # | 검증 항목 | 입력 | 기대 결과 | 구분 |
|---|---------|------|---------|------|
| H1 | 극소 수율 + 대량 부족분 → 계산 결과가 int 범위 초과 감지 | `calculateActualProduction(20_000_000, 0.01)` | 음수 반환 또는 `ArithmeticException` (현재 구현의 오버플로우 버그 노출) | Safety |
| H2 | 오버플로우 발생 시 processNext()가 예외를 던지며 주문 상태를 PRODUCING으로 유지 | yield=0.01, stock=0, quantity=20_000_000 | `processNext()` 예외 발생 + order.getStatus() == PRODUCING (데드락 방지 확인) | Safety |

#### Overflow 수정 후 회귀 검증 (GREEN에서 `long` 또는 범위 체크로 수정)
| # | 검증 항목 | 입력 | 기대 결과 | 구분 |
|---|---------|------|---------|------|
| H3 | 수정 후: 극소 수율 + 대량 부족분 → ArithmeticException (명시적 오류) | `calculateActualProduction(20_000_000, 0.01)` | `ArithmeticException` ("실 생산량이 int 범위를 초과합니다") | Safety |
| H4 | 수정 후: 정상 범위 계산은 여전히 올바른 값 반환 | `calculateActualProduction(10, 0.9)` | `ceil(10/0.81)` = 13 | Regression |

#### 부족분=1 데드락 경계 검증
| # | 검증 항목 | 입력 | 기대 결과 | 구분 |
|---|---------|------|---------|------|
| H5 | 부족분=1, 최대 수율(1.0) → ceil 결과가 1 이상임을 보장 | `calculateActualProduction(1, 1.0)` | 결과 >= 1 이고 stock + result >= quantity (데드락 없음) | Regression |
| H6 | 부족분=1, 최소 수율(0.01) → 대량 생산 후 CONFIRMED 전환 성공 | yield=0.01, stock=0, quantity=1 | processNext() 후 order.getStatus() == CONFIRMED, stock >= 0 | Regression |
| H7 | processNext() 완료 후 stock이 음수가 되지 않음 (공식 안전성 보장) | yield=0.5, stock=0, quantity=5 | processNext() 후 sample.getStock() >= 0 | Regression |

> **GREEN 단계 수정 필요:** `calculateActualProduction()`에서 `(int) Math.ceil(...)` 결과가 `Integer.MAX_VALUE`를 초과하면 `ArithmeticException`을 던지도록 수정

---

### Scenario 5: 영속성 — 재시작 후 데이터 유지
**흐름:** 데이터 저장 → 새 Repository 인스턴스 생성(같은 파일 경로) → 데이터 조회

| # | 검증 항목 | 기대 결과 | 구분 |
|---|---------|---------|------|
| 14 | Sample 저장 후 새 인스턴스로 findById() | 모든 필드 동일 | Regression |
| 15 | Order 저장 후 새 인스턴스로 findById() | status, quantity 등 동일 | Regression |
| 16 | CONFIRMED 상태 Order 저장 후 재로드 | status == CONFIRMED | Regression |

---

## Main.java 구현 (Composition Root)

### MainTest (Main.java 존재 여부 검증)

| # | 검증 항목 | 기대 결과 | 구분 |
|---|---------|---------|------|
| 17 | Main 클래스가 `main(String[] args)` 메서드를 가짐 | 리플렉션으로 확인 | Safety |

### Main.java 설계
```
Main.java 역할:
- JsonSampleRepository, JsonOrderRepository 인스턴스 생성
- ProductionQueue 인스턴스 생성
- 각 Service 인스턴스 생성 (의존성 주입)
- 각 View, Controller 인스턴스 생성
- MainController.run() 호출
```

---

## 테스트 파일 위치

```
src/test/java/com/ssemi/sampleorder/
├── integration/
│   └── IntegrationTest.java     (16개 시나리오 케이스 + 7개 하드 케이스 = 23개)
└── MainTest.java                (1개 Safety 케이스)
```

**총 24개 테스트 케이스** — Safety 4개 / Regression 20개

---

## GREEN 단계 구현 대상

```
src/main/java/com/ssemi/sampleorder/
└── Main.java    — Composition Root, 의존성 조립 및 MainController.run() 호출
```

**ProductionService.calculateActualProduction() 버그 수정 필요:**
```java
// 수정 전 (버그): int 오버플로우 발생 가능
return (int) Math.ceil(shortage / (yield * 0.9));

// 수정 후: long으로 중간 계산 후 범위 초과 시 ArithmeticException
long result = (long) Math.ceil(shortage / (yield * 0.9));
if (result > Integer.MAX_VALUE)
    throw new ArithmeticException("실 생산량이 int 범위를 초과합니다: " + result);
return (int) result;
```
