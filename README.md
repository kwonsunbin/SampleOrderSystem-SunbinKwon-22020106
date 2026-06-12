# 반도체 시료 생산주문관리 시스템 (SampleOrderSystem)

S-Semi 사의 반도체 시료 주문·생산·출고 전 과정을 관리하는 Java 콘솔 애플리케이션입니다.
엑셀/메모장 기반의 수동 관리를 대체하여 주문 누락 방지, 생산 현황 추적, 재고 관리를 체계화합니다.

MVC 패턴과 JSON 파일 기반 영속성을 사용합니다.

---

## 주요 기능

| 기능       | 설명                                                   |
| -------- | ---------------------------------------------------- |
| 시료 관리    | 반도체 시료 등록·조회·이름 검색                                   |
| 주문 접수    | 시료 ID·고객명·수량으로 주문 생성 (`RESERVED` 상태)                 |
| 주문 승인/거절 | 재고 충분 시 즉시 `CONFIRMED`, 부족 시 생산라인(`PRODUCING`) 자동 분기 |
| 생산 라인 관리 | FIFO 큐 기반 생산 처리, 수율 반영 실 생산량 계산 후 `CONFIRMED` 전환     |
| 모니터링     | 상태별 주문 수 집계 및 시료별 재고 현황(`여유/부족/고갈`) 확인               |
| 출고 처리    | `CONFIRMED` 주문을 `RELEASED`로 최종 전환                    |
| 데이터 영속성  | JSON 파일(`data/`) 저장으로 재시작 후에도 데이터 유지                 |

---

## 주문 상태 흐름

```text
RESERVED ──► CONFIRMED ──► RELEASED
    │
    ├──► PRODUCING ──► CONFIRMED ──► RELEASED
    │
    └──► REJECTED
```

---

## 개발 환경

| 항목         | 버전                      |
| ---------- | ----------------------- |
| Java       | 17 이상                   |
| 빌드 도구      | Gradle 8.x (Kotlin DSL) |
| 테스트        | JUnit 5 + Mockito       |
| JSON 라이브러리 | org.json 20240303       |

---

## 실행 방법

### 1. 영속 모드 (기본) — 이전 데이터 유지

```bash
./run.sh
```

`data/samples.json`, `data/orders.json`에 저장된 기존 데이터를 그대로 불러와 시작합니다.

앱 종료 후에도 변경 사항이 파일에 저장되어 다음 실행 시 이어집니다.

### 2. 클린 모드 — 데이터 초기화 후 시작

```bash
./run-clean.sh
```

기존 데이터 파일을 삭제하고 빈 상태로 시작합니다.

처음부터 새로 테스트하고 싶을 때 사용합니다.

> 두 스크립트 모두 빌드(installDist)를 자동으로 수행한 뒤 앱을 실행합니다. 별도의 빌드 단계가 필요 없습니다.

### 실행 화면

```text
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

---

## 데이터 파일 위치

| 파일                  | 내용             |
| ------------------- | -------------- |
| `data/samples.json` | 등록된 시료 목록 및 재고 |
| `data/orders.json`  | 주문 이력 및 상태     |

수동으로 데이터를 수정하려면 앱 종료 후 해당 파일을 직접 편집하면 됩니다.

---

## 테스트 실행

### 전체 테스트 실행

```bash
./gradlew test
```

### 테스트 결과 리포트 열기

```bash
open build/reports/tests/test/index.html
```

### 특정 테스트 클래스만 실행

```bash
./gradlew test --tests "com.ssemi.sampleorder.service.OrderServiceTest"

./gradlew test --tests "com.ssemi.sampleorder.integration.IntegrationTest"
```

### 테스트 구성

```text
src/test/
├── model/
│   ├── OrderTest.java
│   ├── SampleTest.java
│   ├── ProductionQueueTest.java
│   └── StockStatusTest.java
├── repository/
│   ├── JsonSampleRepositoryTest.java
│   └── JsonOrderRepositoryTest.java
├── service/
│   ├── OrderServiceTest.java
│   ├── ProductionServiceTest.java
│   ├── SampleServiceTest.java
│   └── MonitorServiceTest.java
├── controller/
└── integration/
    └── IntegrationTest.java
```

---

## 데이터 파일 구조

```text
data/
├── samples.json
└── orders.json
```

### samples.json 예시

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

### orders.json 예시

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

## 프로젝트 구조

```text
src/main/java/com/ssemi/sampleorder/
├── Main.java               # 진입점 (--clean 플래그 처리)
├── model/                  # 도메인 모델
├── repository/             # JSON 기반 데이터 접근 계층
├── service/                # 비즈니스 로직
├── controller/             # 사용자 입력 처리 및 상태 전이
└── view/                   # 콘솔 UI

data/                       # 영속성 데이터
docs/                       # 설계 문서
```

---

## 수율 계산 공식

생산라인에서 재고 부족분을 채울 때 공정 손실을 고려한 생산량을 계산합니다.

```text
실 생산량  = ⌈ 부족분 / (수율 × 0.9) ⌉
총 생산시간 = 평균 생산시간 × 실 생산량
```

예시:

```text
부족분 10개, 수율 0.9
→ ⌈10 / 0.81⌉ = 13개 생산
```

---

## 개발 방법론

Agentic TDD (RED → GREEN → REVIEW)

각 개발 단계를 전문화된 서브에이전트(test-agent, impl-agent, review-agent)가 담당하며, Phase별 독립적인 TDD 사이클을 통해 개발했습니다.

자세한 기능 명세는 `docs/PRD.md`를, 아키텍처 설명은 `docs/ARCHITECTURE.md`를 참고하세요.
