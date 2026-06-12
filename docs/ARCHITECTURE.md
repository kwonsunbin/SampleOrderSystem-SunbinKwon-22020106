# ARCHITECTURE.md — 반도체 시료 생산주문관리 시스템

## 1. 시스템 개요

S-Semi 사의 반도체 시료(Sample) 주문·생산·출고 전 과정을 관리하는 **Java 콘솔 애플리케이션**이다.
MVC 패턴으로 계층을 분리하고, JSON 파일 기반 영속성으로 재시작 후에도 데이터를 유지한다.

---

## 2. 레이어 구조

```
┌──────────────────────────────────────────┐
│                  View                    │  콘솔 I/O 전용 (출력·입력 래핑)
├──────────────────────────────────────────┤
│               Controller                 │  사용자 흐름 중재 (View ↔ Service)
├──────────────────────────────────────────┤
│                Service                   │  비즈니스 규칙 (재고 분기·수율 계산 등)
├──────────────────────────────────────────┤
│              Repository                  │  데이터 접근 추상화 (인터페이스 + JSON 구현체)
├──────────────────────────────────────────┤
│                 Model                    │  순수 도메인 객체 (외부 의존 없음)
└──────────────────────────────────────────┘
```

**의존성 방향 규칙**

```
View → Controller → Service → Repository → Model
```

- View는 Service를 직접 호출하지 않는다.
- Controller는 Repository를 직접 사용하지 않는다.
- Service는 View를 알지 못한다.
- Model은 어떤 계층도 의존하지 않는다.

---

## 3. 패키지 구조

```
src/main/java/com/ssemi/sampleorder/
├── Main.java                          # Composition Root — 모든 의존성 조립
│
├── model/
│   ├── Sample.java                    # 시료 엔티티
│   ├── Order.java                     # 주문 엔티티 (상태 전이 내장)
│   ├── OrderStatus.java               # 주문 상태 Enum
│   ├── StockStatus.java               # 재고 상태 Enum (팩토리 메서드 포함)
│   └── ProductionQueue.java           # FIFO 생산 큐 (ArrayDeque 래핑)
│
├── repository/
│   ├── SampleRepository.java          # 시료 CRUD 인터페이스
│   ├── OrderRepository.java           # 주문 CRUD 인터페이스
│   ├── json/
│   │   ├── JsonSampleRepository.java  # JSON 파일 기반 시료 저장소
│   │   └── JsonOrderRepository.java   # JSON 파일 기반 주문 저장소
│   └── util/
│       └── JsonFileUtil.java          # JSON 직렬화/역직렬화 유틸
│
├── service/
│   ├── SampleService.java             # 시료 등록·조회·검색 + 재고 관리
│   ├── OrderService.java              # 주문 접수·승인·거절·출고
│   ├── ProductionService.java         # 생산 큐 처리 + 수율 계산
│   ├── MonitorService.java            # 상태별 집계 + 재고 상태 판정
│   └── SampleStockInfo.java           # 모니터링용 DTO (시료별 재고 현황)
│
├── controller/
│   ├── MainController.java            # 메인 메뉴 루프
│   ├── SampleController.java          # 시료 관리 흐름
│   ├── OrderController.java           # 주문 접수·승인·거절 흐름
│   ├── ProductionController.java      # 생산라인 실행 흐름
│   ├── MonitorController.java         # 모니터링 조회 흐름
│   └── ReleaseController.java         # 출고 처리 흐름
│
└── view/
    ├── ConsoleView.java               # 공통 I/O 유틸 (Scanner 래핑)
    ├── MenuView.java                  # 메인·서브 메뉴 출력
    ├── SampleView.java                # 시료 목록·검색 결과 출력
    ├── OrderView.java                 # 주문 목록·상세 출력
    ├── ProductionView.java            # 생산 큐·진행 현황 출력
    ├── MonitorView.java               # 상태별 주문 수·재고 현황 출력
    └── ReleaseView.java               # 출고 대상 목록 출력
```

---

## 4. 도메인 모델 상세

### 4.1 Order (주문)

```
필드
  id            : String (UUID)
  sampleId      : String
  customerName  : String
  quantity      : int (1 이상)
  status        : OrderStatus
  createdAt     : LocalDateTime

상태 전이 테이블 (허용된 경로만 통과, 위반 시 IllegalStateException)
  RESERVED  → CONFIRMED / PRODUCING / REJECTED
  PRODUCING → CONFIRMED
  CONFIRMED → RELEASED
  REJECTED  → (없음)
  RELEASED  → (없음)
```

상태 전이 검증은 `Order.transitionTo()` 내부의 `ALLOWED_TRANSITIONS` 맵으로 관리한다.
Service가 아닌 도메인 객체 자신이 전이 규칙을 알고 있어 규칙 위반을 조기에 차단한다.

생성자가 두 가지인 이유: 기본 생성자는 새 주문을 만들 때, 오버로드 생성자는 JSON에서 역직렬화할 때 status·createdAt을 그대로 복원하기 위함이다.

---

### 4.2 Sample (시료)

```
필드
  id                 : String
  name               : String
  avgProductionTime  : int (분, 1 이상)
  yield              : double (0 초과 ~ 1.0 이하)
  stock              : int (0 이상)

뮤터블 필드: stock (addStock / reduceStock으로만 변경)
```

`reduceStock`은 현재 재고를 초과하는 차감을 거부한다.
나머지 필드는 불변(final)으로 선언해 생성 후 변경을 방지한다.

---

### 4.3 ProductionQueue (생산 큐)

`ArrayDeque<Order>`를 래핑한 FIFO 큐.
직접 `ArrayDeque`를 노출하지 않고 `enqueue / dequeue / peek / toList` API만 공개해 내부 구조를 캡슐화한다.

`ProductionQueue`는 **인메모리 객체**다. 애플리케이션 재시작 시 `orders.json`에서 `PRODUCING` 상태 주문을 읽어 큐를 재구성해야 한다 — 이 책임은 `Main.java`의 Composition Root에 있다.

---

### 4.4 StockStatus (재고 상태 Enum)

| 값 | 의미 | 판정 조건 |
|---|---|---|
| `SUFFICIENT` | 여유 | `demand == 0` 또는 `stock >= demand` |
| `SHORTAGE` | 부족 | `0 < stock < demand` |
| `DEPLETED` | 고갈 | `stock == 0` (demand가 있을 때) |

`StockStatus.of(stock, demand)` 팩토리 메서드 하나로 판정 로직을 중앙화했다.

---

## 5. 핵심 비즈니스 로직

### 5.1 주문 승인 분기 (OrderService.approveOrder)

```
재고 >= 주문 수량  →  즉시 CONFIRMED + 재고 차감
재고 < 주문 수량   →  PRODUCING + ProductionQueue에 enqueue
```

재고 차감은 `Sample.reduceStock()`을 통해 도메인 객체가 수행하고,
저장소(sampleRepository)에 즉시 반영해 다음 주문 승인 시에도 갱신된 재고를 읽는다.

---

### 5.2 수율 반영 생산량 계산 (ProductionService)

```
실 생산량  = ⌈ 부족분 / (수율 × 0.9) ⌉
총 생산시간 = 평균 생산시간 × 실 생산량
```

- `0.9` 계수: 공정 손실 10% 추가 버퍼를 항상 반영한다.
- `Math.ceil` 후 결과를 `long`으로 받고 `int` 범위 초과 시 `ArithmeticException`으로 조기 실패한다 (정수 오버플로우 방지).

`processNext()`는 큐에서 주문을 꺼낸 뒤 ① 부족분 계산 → ② 생산량만큼 재고 추가 → ③ 주문 수량만큼 재고 차감 → ④ 상태 CONFIRMED 전이의 순서로 원자적으로 처리한다.

---

### 5.3 모니터링 (MonitorService)

- `getOrderCountByStatus()`: `REJECTED`를 제외한 4개 상태 주문 수를 집계한다.
  주문이 없는 상태도 `0`으로 표시하기 위해 `putIfAbsent`로 기본값을 채운다.
- `getStockStatusBySample()`: `PRODUCING` 상태 주문의 수량 합을 demand로 계산해 시료별 `StockStatus`를 결정한다.

---

## 6. 영속성 계층 설계

### Repository 인터페이스 → JSON 구현체 분리

```
SampleRepository  (interface)
    └── JsonSampleRepository  (구현체, data/samples.json)

OrderRepository  (interface)
    └── JsonOrderRepository  (구현체, data/orders.json)
```

인터페이스를 두는 이유: Service 계층 단위 테스트 시 Mockito로 교체 가능하게 한다.

### JSON 저장 방식

`JsonFileUtil`이 파일 읽기·쓰기를 담당하고, 각 Repository는 `toJson / fromJson`으로 직렬화만 책임진다.
`save()`는 upsert 방식 — 같은 ID가 있으면 덮어쓰고 없으면 append한다.
파일이 없거나 비어 있으면 빈 배열(`[]`)을 반환해 첫 실행을 안전하게 처리한다.

### 데이터 파일

```
data/
├── samples.json   # [{ "id", "name", "avgProductionTime", "yield", "stock" }, ...]
└── orders.json    # [{ "id", "sampleId", "customerName", "quantity", "status", "createdAt" }, ...]
```

---

## 7. Composition Root (Main.java)

의존성 주입을 프레임워크 없이 `Main.java`에서 수동으로 수행한다.

```
JsonSampleRepository ─┐
JsonOrderRepository  ─┼─► OrderService ──► OrderController
ProductionQueue      ─┘       │
                              ▼
                       ProductionService ──► ProductionController
                              │
                       MonitorService   ──► MonitorController
```

`ConsoleView`는 `System.in / System.out`을 주입받아 테스트 시 스트림 교체가 가능하다.

---

## 8. 테스트 전략

| 대상 | 방식 |
|---|---|
| Model (Order, Sample, ProductionQueue, StockStatus) | 순수 단위 테스트 — 외부 의존 없음 |
| Repository (JsonSampleRepository, JsonOrderRepository) | 임시 파일(`@TempDir`)을 이용한 round-trip 테스트 |
| Service (OrderService, ProductionService, MonitorService) | Mockito로 Repository 목킹, 비즈니스 로직만 검증 |
| Controller | Mockito로 Service 목킹, 흐름 제어 검증 |
| Integration (IntegrationTest) | 전체 객체 그래프를 실제 연결해 시나리오 엔드투엔드 검증 |

개발 방법론: **Agentic TDD** (RED → GREEN → REVIEW 사이클, 각 단계를 전문 서브에이전트가 담당)

---

## 9. 주요 설계 의사결정 (ADR 요약)

| 결정 | 이유 |
|---|---|
| JSON 파일 영속성 (DB 미사용) | 외부 인프라 없이 단독 실행 가능, 사람이 읽고 수정 가능 |
| Repository 인터페이스 분리 | Service 테스트 시 실제 파일 I/O 없이 Mockito로 교체 가능 |
| ProductionQueue 인메모리 유지 | FIFO 순서 보장을 JVM 내 `ArrayDeque`로 단순하게 구현, 재시작 시 DB 복원 패턴과 동일 |
| Order 생성자 오버로드 (역직렬화용) | JSON 복원 시 `status`, `createdAt`을 그대로 재현해야 하므로 별도 생성자 필요 |
| `StockStatus.of()` 팩토리 메서드 | 판정 조건이 바뀌어도 수정 포인트가 Enum 하나뿐 |
| `Math.ceil` 후 long 캐스팅 + 오버플로우 검증 | `double → int` 직접 캐스팅 시 정수 오버플로우 묵살 방지 |
| Composition Root (Main.java) | DI 프레임워크 없이 의존성 조립, 테스트와 운영 객체 그래프를 동일 구조로 검증 가능 |
