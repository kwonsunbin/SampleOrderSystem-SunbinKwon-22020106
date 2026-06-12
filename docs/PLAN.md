# PLAN.md — 반도체 시료 생산주문관리 시스템 개발 계획

## 1. 프로젝트 개요

| 항목 | 내용 |
|------|------|
| 프로젝트명 | SampleOrderSystem |
| 플랫폼 | Java 콘솔 애플리케이션 |
| 아키텍처 | MVC (Model-View-Controller) |
| 빌드 도구 | Gradle (Kotlin DSL) |
| 테스트 프레임워크 | JUnit 5 |
| 데이터 영속성 | JSON 파일 기반 |
| 개발 방법론 | Agentic TDD (RED → GREEN → REVIEW 사이클) |

---

## 2. 개발 방법론: Agentic TDD

### 2.1 기본 사이클

```
RED (테스트 작성)  →  GREEN (구현)  →  REVIEW (검토 + 다음 사이클 결정)
```

각 단계는 **전문화된 서브 에이전트**가 담당한다.

| 단계 | 담당 에이전트 | 역할 | Human-in-the-loop |
|------|-------------|------|-------------------|
| RED | `test-agent` | 실패하는 JUnit 5 테스트 작성. 프로덕션 코드 절대 금지 | **검토 필수** — 테스트 계획을 사람이 승인 후 진행 |
| GREEN | `impl-agent` | 테스트를 통과시키는 최소한의 프로덕션 코드 작성 | 자동 진행 |
| REVIEW | `review-agent` | TDD 사이클 준수 여부 검증, APPROVE / REJECT 판정 | **검토 필수** — REJECT 시 RED 단계 재시작 |

### 2.2 운영 원칙

- RED 단계 착수 전 반드시 `PLAN.md`에 테스트 범위와 케이스 목록을 작성하고 사람의 승인을 받는다.
- REVIEW 단계 결과 없이 다음 사이클로 진입하지 않는다.
- 각 Phase 완료 시 `git commit`으로 명확한 이력을 남긴다.
- 커밋 컨벤션: `feat:`, `test:`, `refactor:`, `docs:`, `fix:`

---

## 3. 전체 개발 Phase

```
Phase 0  →  Phase 1  →  Phase 2  →  Phase 3  →  Phase 4  →  Phase 5
환경 구성    도메인 모델  영속성 계층   서비스 계층   MVC 연결     통합 & 완성
 (완료)
```

---

## 4. 소스 구조 (패키지 트리)

```
src/
├── main/java/com/ssemi/sampleorder/
│   ├── Main.java                          # 진입점, 의존성 조립(Composition Root)
│   │
│   ├── model/                             # 도메인 모델 (순수 데이터 + 상태)
│   │   ├── Sample.java                    # 시료 엔티티
│   │   ├── Order.java                     # 주문 엔티티
│   │   ├── OrderStatus.java               # 주문 상태 Enum
│   │   ├── StockStatus.java               # 재고 상태 Enum (여유/부족/고갈)
│   │   └── ProductionQueue.java           # FIFO 생산 큐 (ArrayDeque 래핑)
│   │
│   ├── repository/                        # 데이터 접근 추상화 (인터페이스 + 구현체)
│   │   ├── SampleRepository.java          # 시료 CRUD 인터페이스
│   │   ├── OrderRepository.java           # 주문 CRUD 인터페이스
│   │   ├── json/
│   │   │   ├── JsonSampleRepository.java  # JSON 파일 기반 시료 저장소
│   │   │   └── JsonOrderRepository.java   # JSON 파일 기반 주문 저장소
│   │   └── util/
│   │       └── JsonFileUtil.java          # JSON 직렬화/역직렬화 유틸
│   │
│   ├── service/                           # 비즈니스 로직 (핵심 도메인 규칙)
│   │   ├── SampleService.java             # 시료 등록·조회·검색 + 재고 관리
│   │   ├── OrderService.java              # 주문 접수·승인·거절 + 재고 분기 처리
│   │   ├── ProductionService.java         # 생산라인 큐 관리 + 수율 계산
│   │   └── MonitorService.java            # 상태별 주문 집계 + 재고 상태 판정
│   │
│   ├── controller/                        # 흐름 제어 (View ↔ Service 중재)
│   │   ├── MainController.java            # 메인 메뉴 분기 진입점
│   │   ├── SampleController.java          # 시료 관리 흐름
│   │   ├── OrderController.java           # 주문 접수·승인·거절 흐름
│   │   ├── ProductionController.java      # 생산라인 실행 흐름
│   │   ├── MonitorController.java         # 모니터링 조회 흐름
│   │   └── ReleaseController.java         # 출고 처리 흐름
│   │
│   └── view/                              # 콘솔 I/O (출력 전용, 입력 래핑)
│       ├── ConsoleView.java               # 공통 입출력 유틸 (Scanner 래핑, 공통 출력 형식)
│       ├── MenuView.java                  # 메인·서브 메뉴 출력
│       ├── SampleView.java                # 시료 목록·검색 결과 출력
│       ├── OrderView.java                 # 주문 목록·상세 출력
│       ├── ProductionView.java            # 생산 큐·진행 현황 출력
│       ├── MonitorView.java               # 상태별 주문 수·재고 현황 출력
│       └── ReleaseView.java               # 출고 대상 목록 출력
│
└── test/java/com/ssemi/sampleorder/
    ├── model/
    │   ├── OrderTest.java                 # 상태 전이 유효성 테스트
    │   └── ProductionQueueTest.java       # FIFO 동작, 빈 큐 경계 테스트
    ├── service/
    │   ├── SampleServiceTest.java         # 시료 등록·중복·검색 테스트
    │   ├── OrderServiceTest.java          # 재고 분기(CONFIRMED/PRODUCING) 핵심 테스트
    │   ├── ProductionServiceTest.java     # 수율 계산 공식, ceil 처리 테스트
    │   └── MonitorServiceTest.java        # 상태별 집계, StockStatus 판정 테스트
    └── repository/
        ├── JsonSampleRepositoryTest.java  # JSON 직렬화 왕복(round-trip) 테스트
        └── JsonOrderRepositoryTest.java   # JSON 직렬화 왕복(round-trip) 테스트
```

---

## 5. 객체 분할 설계

### 5.1 Model 계층 — 도메인 엔티티

#### `Sample` (시료)
```
필드: id(String), name(String), avgProductionTime(int, 분), yield(double, 0~1), stock(int)
책임: 시료 데이터 보관, 재고 증감(addStock / reduceStock)
불변 규칙: yield는 0 초과 1 이하, stock은 0 이상
```

#### `Order` (주문)
```
필드: id(String), sampleId(String), customerName(String), quantity(int), status(OrderStatus), createdAt(LocalDateTime)
책임: 주문 데이터 보관, 상태 전이(transitionTo)
불변 규칙: 상태 전이는 허용된 경로만 통과 (RESERVED→CONFIRMED, RESERVED→PRODUCING, RESERVED→REJECTED, PRODUCING→CONFIRMED, CONFIRMED→RELEASED)
```

#### `OrderStatus` (Enum)
```
값: RESERVED, REJECTED, PRODUCING, CONFIRMED, RELEASED
```

#### `StockStatus` (Enum)
```
값: SUFFICIENT(여유), SHORTAGE(부족), DEPLETED(고갈)
판정 기준: stock==0 → DEPLETED, stock<demand → SHORTAGE, else → SUFFICIENT
```

#### `ProductionQueue`
```
내부 구조: ArrayDeque<Order> (FIFO 보장)
책임: enqueue, dequeue, peek, isEmpty, size
불변 규칙: PRODUCING 상태 주문만 큐에 진입 가능
```

---

### 5.2 Repository 계층 — 데이터 접근

#### `SampleRepository` (인터페이스)
```
save(Sample), findById(String), findAll(), findByName(String), delete(String)
```

#### `OrderRepository` (인터페이스)
```
save(Order), findById(String), findAll(), findByStatus(OrderStatus), delete(String)
```

#### `JsonSampleRepository` / `JsonOrderRepository`
```
책임: data/ 디렉터리 내 samples.json / orders.json 파일에 읽기·쓰기
직렬화: 표준 Java JSON 라이브러리 (org.json 또는 Gson) 활용
```

---

### 5.3 Service 계층 — 비즈니스 로직

#### `SampleService`
```
registerSample(name, avgTime, yield) → Sample
listSamples() → List<Sample> (with stock)
searchSample(keyword) → List<Sample>
getStock(sampleId) → int
```

#### `OrderService`  ← 핵심 복잡도 집중
```
reserveOrder(sampleId, customerName, quantity) → Order
approveOrder(orderId) → Order
  ├── 재고 충분 → CONFIRMED + 재고 차감
  └── 재고 부족 → PRODUCING + ProductionQueue.enqueue
rejectOrder(orderId) → Order
listReservedOrders() → List<Order>
```

#### `ProductionService`  ← 수율 계산 집중
```
processNextInQueue() → Order  (다음 PRODUCING 주문 처리)
calculateActualProduction(shortage, yield) → int
  공식: Math.ceil(shortage / (yield * 0.9))
calculateTotalProductionTime(avgTime, actualProduction) → int
getQueueSnapshot() → List<Order>
```

#### `MonitorService`
```
getOrderCountByStatus() → Map<OrderStatus, Long>
getOrdersByStatus(status) → List<Order>
getStockStatusBySample() → List<StockSummary>  (시료별 재고 상태)
determineStockStatus(stock, demand) → StockStatus
```

---

### 5.4 Controller 계층 — 흐름 제어

각 Controller는 해당 도메인의 **사용자 인터랙션 흐름** 하나만 담당한다.

| Controller | 담당 흐름 | 주요 메서드 |
|-----------|----------|-----------|
| `MainController` | 메인 메뉴 루프 | `run()` |
| `SampleController` | 시료 등록·조회·검색 | `handleRegister()`, `handleList()`, `handleSearch()` |
| `OrderController` | 주문 접수·승인·거절 | `handleReserve()`, `handleApprove()`, `handleReject()` |
| `ProductionController` | 생산라인 실행 | `handleProcessQueue()`, `handleViewQueue()` |
| `MonitorController` | 상태·재고 모니터링 | `handleOrderMonitor()`, `handleStockMonitor()` |
| `ReleaseController` | 출고 처리 | `handleRelease()` |

---

### 5.5 View 계층 — 콘솔 I/O

- `ConsoleView`: `Scanner` 래핑, 입력 유틸(`readInt`, `readString`), 공통 출력(`printSuccess`, `printError`, `printTable`)
- 각 도메인 View는 해당 Controller에서만 사용되며 **출력 전용**. 비즈니스 결정을 내리지 않는다.

---

## 6. Phase별 개발 상세 계획

---

### Phase 0: 프로젝트 초기화 ✅ 완료

| 항목 | 상태 |
|------|------|
| Gradle 빌드 설정 (JUnit 5) | 완료 |
| `.gitignore`, `CLAUDE.md` 작성 | 완료 |
| Agentic TDD 에이전트 설정 | 완료 |
| 원격 저장소 push | 완료 |

---

### Phase 1: 도메인 모델 구현

**목표:** 비즈니스 핵심 개념을 코드로 표현. 외부 의존성 없는 순수 Java 객체.

**Agentic TDD 사이클:**

```
[RED]   test-agent  → OrderTest, ProductionQueueTest 작성
[GREEN] impl-agent  → Order, OrderStatus, Sample, StockStatus, ProductionQueue 구현
[REVIEW] review-agent → 상태 전이 규칙, 불변 조건 준수 여부 검증
```

**테스트 케이스 목록 (RED 단계 착수 전 검토):**
- `OrderTest`: 허용된 상태 전이 성공 / 불허 상태 전이 예외 발생
- `ProductionQueueTest`: enqueue→dequeue FIFO 순서 보장 / 빈 큐 dequeue 예외

**완료 기준:**
- [ ] 모든 모델 클래스 JUnit 5 테스트 통과
- [ ] `git commit: feat: 도메인 모델 구현 (Phase 1)`

---

### Phase 2: Repository 계층 구현

**목표:** JSON 파일 기반 데이터 영속성 구현. 인터페이스 뒤에 구현체를 숨겨 테스트 가능성 확보.

**Agentic TDD 사이클:**

```
[RED]   test-agent  → JsonSampleRepositoryTest, JsonOrderRepositoryTest 작성
[GREEN] impl-agent  → JsonSampleRepository, JsonOrderRepository, JsonFileUtil 구현
[REVIEW] review-agent → round-trip 직렬화 정확성, 파일 I/O 예외 처리 검증
```

**테스트 케이스 목록 (RED 단계 착수 전 검토):**
- `save → findById` 왕복 데이터 일치
- `findAll` 복수 엔티티 반환
- `findByStatus(RESERVED)` 필터링 정확성
- 존재하지 않는 ID 조회 시 `Optional.empty()` 반환
- JSON 파일 없을 때 빈 목록 반환 (첫 실행 안전성)

**완료 기준:**
- [ ] 모든 Repository 테스트 통과
- [ ] `data/` 디렉터리 자동 생성 및 JSON 파일 정상 생성 확인
- [ ] `git commit: feat: JSON 파일 기반 영속성 계층 구현 (Phase 2)`

---

### Phase 3: Service 계층 구현

**목표:** 핵심 비즈니스 규칙 구현. 가장 복잡하고 중요한 Phase.

**Agentic TDD 사이클 (서비스별 독립 사이클):**

#### 3-1. OrderService (재고 분기 로직)
```
[RED]   test-agent  → OrderServiceTest 작성 (재고 충분/부족 분기 케이스)
[GREEN] impl-agent  → OrderService.approveOrder() 구현
[REVIEW] review-agent → 재고 차감 정확성, 상태 전이, 큐 진입 조건 검증
```

**테스트 케이스 목록 (RED 단계 착수 전 검토):**
- 재고 충분 시 `approveOrder` → 상태 CONFIRMED, 재고 차감됨
- 재고 부족 시 `approveOrder` → 상태 PRODUCING, ProductionQueue에 진입
- 재고 정확히 0 시 `approveOrder` → PRODUCING (경계값)
- RESERVED가 아닌 주문 승인 시도 → 예외
- `rejectOrder` → 상태 REJECTED, 재고 변동 없음

#### 3-2. ProductionService (수율 계산)
```
[RED]   test-agent  → ProductionServiceTest 작성 (수율 공식 케이스)
[GREEN] impl-agent  → ProductionService 구현 (수율 계산 + 큐 처리)
[REVIEW] review-agent → ceil 처리, 수율 0.9 계수 정확성 검증
```

**테스트 케이스 목록 (RED 단계 착수 전 검토):**
- `calculateActualProduction(10, 0.9)` → `Math.ceil(10 / 0.81)` = 13
- `calculateActualProduction(1, 1.0)` → `Math.ceil(1 / 0.9)` = 2 (ceil 처리)
- `calculateActualProduction(9, 0.9)` → `Math.ceil(9 / 0.81)` = 12
- `processNextInQueue` → 큐에서 꺼내 CONFIRMED 전환, 재고 반영
- 빈 큐에서 `processNextInQueue` → 적절한 예외/Optional 반환

#### 3-3. MonitorService
```
[RED]   test-agent  → MonitorServiceTest 작성
[GREEN] impl-agent  → MonitorService 구현
[REVIEW] review-agent → REJECTED 제외 여부, StockStatus 판정 경계값 검증
```

**테스트 케이스 목록 (RED 단계 착수 전 검토):**
- 모니터링 집계에서 REJECTED 주문 제외 확인
- `stock==0` → DEPLETED, `0 < stock < demand` → SHORTAGE, `stock >= demand` → SUFFICIENT

**완료 기준:**
- [ ] 모든 Service 테스트 통과
- [ ] 수율 계산 ceil 처리 정확성 검증 완료
- [ ] `git commit: feat: 서비스 계층 비즈니스 로직 구현 (Phase 3)`

---

### Phase 4: Controller + View 계층 구현

**목표:** MVC 연결 완성. 사용자가 실제로 사용할 수 있는 콘솔 애플리케이션 완성.

**Agentic TDD 사이클:**

```
[RED]   test-agent  → Controller 통합 테스트 작성 (Service mock 활용)
[GREEN] impl-agent  → 모든 Controller, View, Main.java 구현
[REVIEW] review-agent → UI 흐름 완전성, Service 분리 원칙 준수 검증
```

**구현 우선순위:**
1. `ConsoleView` (공통 기반)
2. `MenuView` + `MainController` (메인 루프)
3. `SampleController` + `SampleView` (시료 관리)
4. `OrderController` + `OrderView` (주문 접수·승인·거절)
5. `MonitorController` + `MonitorView` (모니터링)
6. `ProductionController` + `ProductionView` (생산라인)
7. `ReleaseController` + `ReleaseView` (출고)

**메인 메뉴 구조:**
```
=== 반도체 시료 생산주문관리 시스템 ===
1. 시료 관리
2. 주문 접수
3. 주문 승인/거절
4. 생산 라인 관리
5. 모니터링
6. 출고 처리
0. 종료
```

**완료 기준:**
- [ ] 전체 메뉴 흐름 수동 동작 확인
- [ ] 비즈니스 로직이 View에 없음 확인
- [ ] `git commit: feat: MVC Controller/View 계층 구현 (Phase 4)`

---

### Phase 5: 통합 검증 및 완성

**목표:** 전체 시나리오 검증, 리팩토링, 최종 PR.

**검증 시나리오:**
1. 시료 등록 → 재고 0인 상태에서 주문 접수 → 승인 → PRODUCING 진입
2. 생산라인 실행 → CONFIRMED 전환 → 출고 → RELEASED
3. 재고 충분 상태에서 승인 → 즉시 CONFIRMED
4. 주문 거절 → 모니터링에서 미표시 확인
5. 애플리케이션 재시작 후 데이터 유지(영속성) 확인

**완료 기준:**
- [ ] 5개 시나리오 모두 정상 동작
- [ ] 모든 테스트 통과 (`./gradlew test`)
- [ ] `git commit: refactor: 통합 검증 및 최종 정리 (Phase 5)`
- [ ] **최종 PR 생성**

---

## 7. 의존성 방향 규칙

```
View  →  Controller  →  Service  →  Repository  →  Model
                    ↘              ↗
                      (직접 접근 금지)
```

- View는 Service를 직접 호출하지 않는다.
- Controller는 Repository를 직접 사용하지 않는다.
- Service는 View를 알지 못한다.
- Model은 어떤 계층도 의존하지 않는다 (최하단 순수 객체).

---

## 8. 데이터 파일 구조

```
data/
├── samples.json    # 시료 목록 (id, name, avgProductionTime, yield, stock)
└── orders.json     # 주문 목록 (id, sampleId, customerName, quantity, status, createdAt)
```

JSON 스키마 예시:
```json
// samples.json
[
  { "id": "S001", "name": "GaAs 웨이퍼 A급", "avgProductionTime": 120, "yield": 0.85, "stock": 50 }
]

// orders.json
[
  { "id": "O001", "sampleId": "S001", "customerName": "서울대 나노연구소", "quantity": 30, "status": "CONFIRMED", "createdAt": "2026-06-12T09:00:00" }
]
```

---

## 9. 주요 체크리스트

| Phase | 항목 | 완료 |
|-------|------|------|
| 0 | 프로젝트 초기화 및 push | ✅ |
| 1 | 도메인 모델 + 테스트 | ☐ |
| 2 | JSON Repository + 테스트 | ☐ |
| 3 | Service 비즈니스 로직 + 테스트 (수율 계산 포함) | ☐ |
| 4 | Controller + View MVC 연결 | ☐ |
| 5 | 통합 검증 + 최종 PR | ☐ |

---

*마지막 업데이트: 2026-06-12*
