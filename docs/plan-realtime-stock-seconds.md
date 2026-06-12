# Test Plan: 실시간 재고 반영 + 초 단위 생산 시간 전환

## 배경

현재 생산 시간 단위가 **분(minutes)** 기반이라 실제 눈으로 확인하기 어렵다.
또한 `completeProductionIfReady()`가 생산라인 메뉴(`[5]`)에서만 호출되어,
모니터링 화면 등 다른 화면에서는 재고가 실시간으로 갱신되지 않는다.

---

## 변경 범위 (단위 변환 + 실시간 반영)

| 레이어 | 변경 내용 |
|--------|-----------|
| `Order` | `totalProductionMinutes` → `totalProductionSeconds` (필드 rename) |
| `ProductionService` | `toMinutes()` → `toSeconds()`, `plusMinutes()` → `plusSeconds()` |
| `ProductionProgressInfo` | `elapsedMinutes` → `elapsedSeconds` |
| `ProductionController` | ETA row 빌드 시 `plusSeconds()`, 표시 단위 "초" |
| `ProductionView` | `"min"` → `"sec"`, 시각 포맷 `HH:mm:ss` |
| `JsonOrderRepository` | JSON key `totalProductionMinutes` → `totalProductionSeconds` (구 key fallback 유지) |
| `Sample.avgProductionTime` | 주석 "분 단위" → "초 단위" (실제 필드는 공유) |
| `MainController` | 루프 시작마다 `productionService.completeProductionIfReady(clock)` 호출 |
| `samples.json` | `avgProductionTime` 값을 5~15 초 단위로 변경 |

---

## 테스트 대상

- `ProductionService` — `completeProductionIfReady`, `startNextProduction`, `getCurrentlyProducingInfo`
- `Order` — `getTotalProductionSeconds()` getter 존재 확인
- `ProductionProgressInfo` — `getElapsedSeconds()` getter 존재 확인
- `MainController` — 메인 루프에서 `completeProductionIfReady` 호출 여부 (행동 기반 테스트)

---

## 테스트 케이스 목록

### A. Order — 초 단위 필드 rename

| # | 테스트 이름 | 입력 | 기대 결과 |
|---|------------|------|-----------|
| A1 | `startProduction` 후 `getTotalProductionSeconds()` 반환 | startedAt, totalSeconds=30 | `getTotalProductionSeconds() == 30` |
| A2 | 이전 코드 `getTotalProductionMinutes()` 접근 시 컴파일 오류 확인 (삭제됨) | — | 메서드 없음 |

### B. ProductionService — `completeProductionIfReady` 초 단위

| # | 테스트 이름 | 입력 | 기대 결과 |
|---|------------|------|-----------|
| B1 | 경과 시간(초) < totalProductionSeconds → PRODUCING 유지 | start=T, total=30s, clock=T+15s | status == PRODUCING |
| B2 | 경과 시간(초) == totalProductionSeconds → CONFIRMED | start=T, total=30s, clock=T+30s | status == CONFIRMED |
| B3 | 경과 시간(초) > totalProductionSeconds → CONFIRMED | start=T, total=30s, clock=T+45s | status == CONFIRMED |
| B4 | 완료 시 재고 정확히 반영 | stock=0, qty=10, yield=1.0, total=12*avgSec | finalStock == 2 |

### C. ProductionService — `startNextProduction` 초 단위

| # | 테스트 이름 | 입력 | 기대 결과 |
|---|------------|------|-----------|
| C1 | totalProductionSeconds 계산 정확 (shortage>0) | stock=0, qty=10, yield=1.0, avgSec=5 | `getTotalProductionSeconds() == 5*12=60` |
| C2 | totalProductionSeconds 계산 정확 (shortage=0) | stock=20, qty=10, avgSec=5 | `getTotalProductionSeconds() == 5*10=50` |

### D. ProductionService — `getCurrentlyProducingInfo` 초 단위

| # | 테스트 이름 | 입력 | 기대 결과 |
|---|------------|------|-----------|
| D1 | progressPercent 50% (초 기반) | start=T, total=30s, clock=T+15s | progressPercent==50 |
| D2 | progressPercent 100% cap (초 기반) | start=T, total=30s, clock=T+60s | progressPercent==100 |
| D3 | estimatedCompletionTime = startedAt + totalSeconds | start=T, total=30s | eta == T+30s |
| D4 | elapsedSeconds 반환 | start=T, clock=T+15s | getElapsedSeconds()==15 |

### E. MainController — 루프마다 완료 체크

| # | 테스트 이름 | 입력 | 기대 결과 |
|---|------------|------|-----------|
| E1 | 메인루프 선택 시 completeProductionIfReady 호출됨 | PRODUCING 주문 있고 완료 시간 경과 | 모니터링 메뉴 진입 전 CONFIRMED로 전환됨 |

---

## 엣지 케이스

- 기존 `orders.json`에 `totalProductionMinutes` 키가 남아있을 경우 → `fromJson`에서 fallback 처리
- `totalProductionSeconds == 0` 상태에서 `getCurrentlyProducingInfo` 호출 → progressPercent=0, eta=now
- PRODUCING 주문이 없는 상태에서 `completeProductionIfReady` 호출 → 예외 없이 무시 (기존 동작 유지)

---

## 테스트 불가 항목 (이유)

- `samples.json` 값 변경 — 데이터 파일이므로 단위 테스트 대상 아님 (수동 확인)
- `ProductionView` 표시 형식(`"sec"`, `HH:mm:ss`) — 콘솔 View 레이어는 UI 테스트 범위 밖
- `MainController.run()` 전체 흐름 — 무한루프 + Scanner 의존, 통합 테스트로만 가능

---

## 영향받는 기존 테스트

`ProductionServiceTest` 내 아래 케이스들은 기존 `toMinutes/plusMinutes` 기반 → 초 기반으로 교체:
- `notReadyYetKeepsProducing` → `plusSeconds(15)`, total=30
- `exactTimeCompletesProduction` → `plusSeconds(30)`, total=30
- `overTimeCompletesProduction` → `plusSeconds(45)`, total=30
- `completionUpdatesStockCorrectly` → totalSeconds=60 (12*5초)
- `startNextRecordsTotalProductionMinutes` → `startNextRecordsTotalProductionSeconds`, totalSeconds=60 (12*5초)
- `startNextNoShortage` → totalSeconds=50 (10*5초)
- `progressPercentFiftyPercent` → `plusSeconds(15)`, total=30
- `progressPercentCappedAt100` → `plusSeconds(60)`, total=30
- `estimatedCompletionTimeIsCorrect` → `plusSeconds(30)`, total=30
