# 반도체 시료 생산주문관리 시스템 (SampleOrderSystem)

S-Semi 사의 반도체 시료 주문·생산·출고 전 과정을 관리하는 Java 콘솔 애플리케이션입니다.
엑셀/메모장 기반의 수동 관리를 대체하여 주문 누락 방지, 생산 현황 추적, 재고 관리를 체계화합니다.

---

## 주요 기능

| 기능 | 설명 |
|---|---|
| 시료 관리 | 반도체 시료 등록·조회·이름 검색 |
| 주문 접수 | 시료 ID·고객명·수량으로 주문 생성 (`RESERVED` 상태) |
| 주문 승인/거절 | 재고 충분 시 즉시 `CONFIRMED`, 부족 시 생산라인(`PRODUCING`) 자동 분기 |
| 생산 라인 관리 | FIFO 큐 기반 생산 처리, 수율 반영 실 생산량 계산 후 `CONFIRMED` 전환 |
| 모니터링 | 상태별 주문 수 집계 및 시료별 재고 현황(`여유/부족/고갈`) 확인 |
| 출고 처리 | `CONFIRMED` 주문을 `RELEASED`로 최종 전환 |
| 데이터 영속성 | JSON 파일(`data/`) 저장으로 재시작 후에도 데이터 유지 |

### 주문 상태 흐름

```
RESERVED ──► CONFIRMED ──► RELEASED
    │
    ├──► PRODUCING ──► CONFIRMED ──► RELEASED
    │
    └──► REJECTED
```

---

## 개발 환경

| 항목 | 버전 |
|---|---|
| Java | 17 이상 |
| 빌드 도구 | Gradle 8.x (Kotlin DSL) |
| 테스트 | JUnit 5 + Mockito |
| JSON 라이브러리 | org.json 20240303 |

---

## 빌드

```bash
# 프로젝트 루트에서 실행
./gradlew build
```

빌드 결과물: `build/libs/SampleOrderSystem-SunbinKwon-22020106-1.0-SNAPSHOT.jar`

---

## 실행

### Gradle로 직접 실행

```bash
./gradlew run
```

> `run` 태스크가 없는 경우 아래 jar 실행 방식을 사용하세요.

### Jar 실행

```bash
./gradlew build
java -cp build/libs/SampleOrderSystem-SunbinKwon-22020106-1.0-SNAPSHOT.jar:build/libs/* com.ssemi.sampleorder.Main
```

### 실행 화면

```
=== 반도체 시료 생산주문관리 시스템 ===
1. 시료 관리
2. 주문 접수
3. 주문 승인/거절
4. 생산 라인 관리
5. 모니터링
6. 출고 처리
0. 종료
선택:
```

> 처음 실행 시 `data/` 디렉터리가 자동으로 생성됩니다.

---

## 테스트 실행

### 전체 테스트 실행

```bash
./gradlew test
```

### 테스트 결과 리포트 열기

```bash
# 테스트 완료 후 아래 경로에서 HTML 리포트 확인 (브라우저로 열기)
open build/reports/tests/test/index.html
```

### 특정 테스트 클래스만 실행

```bash
# 예: OrderService 테스트만 실행
./gradlew test --tests "com.ssemi.sampleorder.service.OrderServiceTest"

# 예: 통합 테스트만 실행
./gradlew test --tests "com.ssemi.sampleorder.integration.IntegrationTest"
```

### 테스트 구성

```
src/test/
├── model/
│   ├── OrderTest.java              # 상태 전이 규칙 검증
│   ├── SampleTest.java             # 재고 증감 유효성 검증
│   ├── ProductionQueueTest.java    # FIFO 동작, 빈 큐 경계 테스트
│   └── StockStatusTest.java        # 재고 상태 판정 경계값 테스트
├── repository/
│   ├── JsonSampleRepositoryTest.java  # JSON 직렬화 round-trip 테스트
│   └── JsonOrderRepositoryTest.java   # JSON 직렬화 round-trip 테스트
├── service/
│   ├── OrderServiceTest.java       # 재고 분기(CONFIRMED/PRODUCING) 핵심 테스트
│   ├── ProductionServiceTest.java  # 수율 계산 공식, ceil 처리 테스트
│   ├── SampleServiceTest.java      # 시료 등록·중복·검색 테스트
│   └── MonitorServiceTest.java     # 상태별 집계, StockStatus 판정 테스트
├── controller/
│   └── (각 Controller 흐름 테스트)
└── integration/
    └── IntegrationTest.java        # 전체 시나리오 엔드투엔드 검증
```

---

## 데이터 파일 구조

애플리케이션 실행 시 프로젝트 루트의 `data/` 디렉터리에 JSON 파일이 생성됩니다.

```
data/
├── samples.json   # 시료 목록
└── orders.json    # 주문 목록
```

**samples.json 예시**
```json
[
  {
    "id": "S001",
    "name": "GaAs 웨이퍼 A급",
    "avgProductionTime": 120,
    "yield": 0.85,
    "stock": 50
  }
]
```

**orders.json 예시**
```json
[
  {
    "id": "O001",
    "sampleId": "S001",
    "customerName": "서울대 나노연구소",
    "quantity": 30,
    "status": "CONFIRMED",
    "createdAt": "2026-06-12T09:00:00"
  }
]
```

---

## 프로젝트 구조 (요약)

```
src/main/java/com/ssemi/sampleorder/
├── Main.java          # 진입점 & 의존성 조립
├── model/             # 도메인 객체 (Sample, Order, ProductionQueue 등)
├── repository/        # 데이터 접근 인터페이스 + JSON 구현체
├── service/           # 비즈니스 로직 (수율 계산, 재고 분기 등)
├── controller/        # 사용자 흐름 제어
└── view/              # 콘솔 입출력
```

설계 상세는 [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)를 참고하세요.

---

## 수율 계산 공식

생산라인에서 재고 부족분을 채울 때 공정 손실을 고려한 생산량을 계산합니다.

```
실 생산량  = ⌈ 부족분 / (수율 × 0.9) ⌉
총 생산시간 = 평균 생산시간(분) × 실 생산량
```

예시: 부족분 10개, 수율 0.9 → `⌈ 10 / 0.81 ⌉ = 13개`

---

## 개발 방법론

**Agentic TDD** (RED → GREEN → REVIEW 사이클)

각 개발 단계를 전문화된 서브에이전트(test-agent, impl-agent, review-agent)가 담당하며,
Phase별로 독립적인 TDD 사이클을 거쳐 코드를 작성했습니다. 상세 계획은 [docs/PLAN.md](docs/PLAN.md)를 참고하세요.
