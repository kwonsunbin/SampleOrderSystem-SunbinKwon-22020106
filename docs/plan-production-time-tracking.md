# Test Plan: 시간 기반 생산 진행 추적 및 생산라인 조회

## 배경 및 목표

현재 `processNext()`는 주문을 즉시 CONFIRMED로 전환한다.  
이번 기능은 **단일 생산 라인** 모델을 도입하여:
1. 주문이 생산 시작 시 `PRODUCING` 상태로 전환되고 시작 시각 + 총 생산 시간이 기록된다.
2. 경과 시간을 체크해 생산이 완료되면 재고 반영 후 `CONFIRMED`로 전환된다.
3. 생산라인 조회 시 **현재 생산 중인 주문**의 진행률/예정 완료 시각과 **대기 중인 주문 목록**의 예상 완료 시각을 함께 표시한다.

---

## 변경 범위 요약

| 레이어 | 변경 내용 |
|--------|-----------|
| `Order` (Model) | `productionStartedAt`, `totalProductionMinutes` 필드 추가 |
| `ProductionService` | `startNextProduction()`, `completeProductionIfReady()`, `getCurrentlyProducingInfo()` 추가 |
| `ProductionProgressInfo` | 진행 정보 VO 신규 생성 |
| `ProductionView` | 현재 생산 중 + 대기 큐 (예상 완료 시각 포함) 화면 추가 |
| `ProductionController` | 메뉴 진입 시 `completeProductionIfReady()` 호출, 화면 갱신 |

---

## Cycle 1 — Order 시간 필드 + ProductionService 시간 인식 로직

### 테스트 대상
- `Order.startProduction(LocalDateTime, int)` — 시작 시각 및 총 생산 시간 기록
- `ProductionService.startNextProduction(Clock)` — 큐 첫 번째 RESERVED 주문을 PRODUCING으로 전환
- `ProductionService.completeProductionIfReady(Clock)` — 경과 시간 체크 후 재고 반영 + CONFIRMED 전환
- `ProductionService.getCurrentlyProducingInfo(Clock)` — 현재 PRODUCING 주문의 진행 정보 반환

### 테스트 케이스 목록

| # | 테스트 이름 | 입력 | 기대 결과 |
|---|------------|------|-----------|
| 1 | startProduction → productionStartedAt 기록됨 | startedAt = T, minutes = 60 | `getProductionStartedAt() == T` |
| 2 | startProduction → totalProductionMinutes 기록됨 | minutes = 60 | `getTotalProductionMinutes() == 60` |
| 3 | startProduction을 PRODUCING이 아닌 상태에서 호출 → 예외 | status = RESERVED | `IllegalStateException` |
| 4 | startNextProduction → 큐 첫 번째 RESERVED 주문이 PRODUCING으로 전환 | 큐에 RESERVED 주문 2건 | 첫 번째 order.status == PRODUCING |
| 5 | startNextProduction → productionStartedAt이 현재 Clock 시각 | clock.now() = T | `getProductionStartedAt() == T` |
| 6 | startNextProduction → totalProductionMinutes 계산 값 기록 | shortage=10, yield=1.0, avgTime=60 → actual=12 → total=720 | `getTotalProductionMinutes() == 720` |
| 7 | startNextProduction → 재고가 충분한 경우 (shortage=0) totalProductionMinutes = avgTime * quantity | stock≥qty | actual=qty, total=avgTime*qty |
| 8 | startNextProduction → 큐 비어있으면 예외 | queue empty | `IllegalStateException` |
| 9 | startNextProduction → 이미 PRODUCING 중이면 예외 | PRODUCING order 존재 | `IllegalStateException("이미 생산 중인 주문이 있습니다.")` |
| 10 | completeProductionIfReady → 경과 시간 < totalProductionMinutes → 아무 것도 안 함 | elapsed=30 < total=60 | status 여전히 PRODUCING |
| 11 | completeProductionIfReady → 경과 시간 >= totalProductionMinutes → CONFIRMED 전환 | elapsed=60 >= total=60 | status == CONFIRMED |
| 12 | completeProductionIfReady → 재고 정확히 반영 (addStock → reduceStock) | stock=0, qty=10, yield=1.0, total=720분 경과 | finalStock == 2 |
| 13 | completeProductionIfReady → PRODUCING 주문 없으면 아무 것도 안 함 | no PRODUCING order | 예외 없음, 변경 없음 |
| 14 | getCurrentlyProducingInfo → PRODUCING 없으면 Optional.empty() | no PRODUCING order | `Optional.empty()` |
| 15 | getCurrentlyProducingInfo → progressPercent 계산 (elapsed/total*100) | elapsed=30, total=60 | progressPercent == 50 |
| 16 | getCurrentlyProducingInfo → progressPercent 100 cap (elapsed > total) | elapsed=90, total=60 | progressPercent == 100 |
| 17 | getCurrentlyProducingInfo → estimatedCompletionTime = startedAt + totalMinutes | startedAt=T, total=60 | completionTime == T+60분 |

---

## Cycle 2 — ProductionView 진행 화면

### 테스트 대상
- `ProductionView.printCurrentlyProducing(ProductionProgressInfo)` — 진행 중 주문 표시
- `ProductionView.printQueueWithEta(List<String[]>)` — 대기 큐 + 예상 완료 시각 표시

### 테스트 케이스 목록

| # | 테스트 이름 | 입력 | 기대 결과 |
|---|------------|------|-----------|
| 18 | printCurrentlyProducing → "현재 처리 중" 헤더 포함 | progressInfo | 출력에 "현재 처리 중" 포함 |
| 19 | printCurrentlyProducing → 진행률 % 포함 | progressPercent=72 | 출력에 "72%" 포함 |
| 20 | printCurrentlyProducing → 예정 완료 시각 포함 | completionTime=09:49 | 출력에 "09:49" 포함 |
| 21 | printCurrentlyProducing → 프로그레스 바 포함 | progressPercent=50 | 출력에 "█" 또는 "░" 포함 |
| 22 | printQueueWithEta → 빈 큐이고 PRODUCING도 없으면 "대기 주문 없음" | empty | "대기 중인 주문이 없습니다" 포함 |
| 23 | printQueueWithEta → 예상 완료 시각 컬럼 포함 | eta list | 출력에 "예상 완료" 컬럼 포함 |

---

## 엣지 케이스

- `totalProductionMinutes == 0`: avgProductionTime=0 불가 (Sample 생성 시 검증), 실생산량=0일 때만 발생 가능 → shortage=0이고 qty>0이면 actualProd=qty, time = avgTime*qty ≥ avgTime ≥ 1 → 발생 불가
- `elapsed > total`: progressPercent는 100으로 캡핑
- 동시에 두 주문이 PRODUCING 상태: `startNextProduction` 시 PRODUCING 주문 존재 여부 체크로 방지
- PRODUCING 주문의 sampleId가 존재하지 않는 경우: `completeProductionIfReady`에서 예외 throw

---

## 테스트 불가 항목

- 실제 시간 경과에 따른 자동 완료: 콘솔 앱 특성상 메뉴 진입 시 `completeProductionIfReady()` 호출로 대체. `Clock` 추상화로 테스트에서 시간 제어 가능.
- 화면 색상/ANSI 코드 렌더링: ConsoleView 출력 캡처로 텍스트 내용만 검증

---

## Clock 전략

- `java.time.Clock`을 `ProductionService`와 `ProductionProgressInfo`에 주입
- 테스트에서 `Clock.fixed(instant, ZoneId.systemDefault())`로 시간 고정
- 프로덕션에서는 `Clock.systemDefaultZone()` 사용
