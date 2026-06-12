# Phase 4 테스트 계획 (RED Phase)

## 대상 클래스

### Controller 계층
- `MainController` — 메인 메뉴 루프, 서브 Controller 분기
- `SampleController` — 시료 등록·조회·검색 흐름
- `OrderController` — 주문 접수·승인·거절 흐름
- `ProductionController` — 생산라인 실행·큐 조회 흐름
- `MonitorController` — 상태별 주문 수·재고 현황 조회 흐름
- `ReleaseController` — 출고 처리 흐름

### View 계층 (테스트 대상 아님 — 출력 전용)
- `ConsoleView`, `MenuView`, `SampleView`, `OrderView`, `ProductionView`, `MonitorView`, `ReleaseView`

---

## 설계 방향

### 테스트 전략
- Service 계층은 **Mockito mock**으로 대체 — Controller 단위 테스트
- 콘솔 입력: `ByteArrayInputStream`으로 시뮬레이션
- 콘솔 출력: `ByteArrayOutputStream`으로 캡처 후 문자열 검증
- `ConsoleView`에 `InputStream` / `PrintStream` 주입 가능하도록 생성자 설계

### build.gradle.kts 추가 의존성
```kotlin
testImplementation("org.mockito:mockito-core:5.11.0")
```

### 의존성 방향 원칙 (테스트로 검증)
- Controller는 Service 메서드를 위임 호출하는지 확인 (`verify(mockService).method(...)`)
- Controller는 Repository를 직접 사용하지 않음
- View는 출력만 담당 — 비즈니스 결정 없음

---

## 1. SampleControllerTest

### Regression: 시료 등록
| # | 테스트명 | 시뮬레이션 입력 | 기대 동작 | 구분 |
|---|---------|--------------|---------|------|
| 1 | 유효한 입력으로 시료 등록 성공 | `"GaAs 웨이퍼\n120\n0.85\n50\n"` | `sampleService.register("GaAs 웨이퍼", 120, 0.85, 50)` 호출됨 | Regression |
| 2 | 등록 후 성공 메시지 출력 | 유효한 입력 | 출력에 "등록" 또는 시료명 포함 | Regression |

### Regression: 시료 목록 조회
| # | 테스트명 | Mock 반환값 | 기대 동작 | 구분 |
|---|---------|-----------|---------|------|
| 3 | 시료 목록 조회 시 sampleService.listSamples() 호출 | `[sample1, sample2]` | `listSamples()` 1회 호출됨 | Regression |
| 4 | 시료 목록이 빈 경우 안내 메시지 출력 | `[]` | 출력에 "없" 또는 "empty" 포함 | Regression |

### Regression: 시료 검색
| # | 테스트명 | 시뮬레이션 입력 | 기대 동작 | 구분 |
|---|---------|--------------|---------|------|
| 5 | 키워드 입력 후 searchSample() 호출 | `"GaAs\n"` | `sampleService.searchSample("GaAs")` 호출됨 | Regression |
| 6 | 검색 결과 없을 때 안내 메시지 출력 | `"없는키워드\n"`, mock `[]` | 출력에 "없" 포함 | Regression |

### Safety: 입력 유효성
| # | 테스트명 | 시뮬레이션 입력 | 기대 동작 | 구분 |
|---|---------|--------------|---------|------|
| 7 | sampleService.register()가 예외 던지면 에러 메시지 출력 | 유효한 입력, mock throws `IllegalArgumentException` | 출력에 오류 메시지 포함, 앱 종료 안 됨 | Safety |

---

## 2. OrderControllerTest

### Regression: 주문 접수
| # | 테스트명 | 시뮬레이션 입력 | 기대 동작 | 구분 |
|---|---------|--------------|---------|------|
| 1 | 유효한 입력으로 주문 접수 | `"S001\n서울대\n10\n"` | `orderService.reserveOrder("S001", "서울대", 10)` 호출됨 | Regression |
| 2 | 접수 후 성공 메시지 출력 | 유효한 입력 | 출력에 주문 ID 또는 "접수" 포함 | Regression |

### Regression: 주문 승인
| # | 테스트명 | Mock 반환값 / 입력 | 기대 동작 | 구분 |
|---|---------|-----------------|---------|------|
| 3 | RESERVED 주문 목록 표시 후 승인 | mock `[order1]`, 입력 `"O001\n"` | `orderService.approveOrder("O001")` 호출됨 | Regression |
| 4 | CONFIRMED 결과 출력 | approve mock → CONFIRMED 주문 | 출력에 "승인" 또는 "CONFIRMED" 포함 | Regression |
| 5 | PRODUCING 결과 출력 (재고 부족) | approve mock → PRODUCING 주문 | 출력에 "생산" 또는 "PRODUCING" 포함 | Regression |

### Regression: 주문 거절
| # | 테스트명 | Mock 반환값 / 입력 | 기대 동작 | 구분 |
|---|---------|-----------------|---------|------|
| 6 | 주문 거절 | mock `[order1]`, 입력 `"O001\n"` | `orderService.rejectOrder("O001")` 호출됨 | Regression |
| 7 | 거절 후 메시지 출력 | reject mock → REJECTED 주문 | 출력에 "거절" 또는 "REJECTED" 포함 | Regression |

### Safety: 예외 처리
| # | 테스트명 | 조건 | 기대 동작 | 구분 |
|---|---------|------|---------|------|
| 8 | 대기 주문 없을 때 승인 화면 진입 | `listReservedOrders()` → `[]` | 출력에 "없" 포함, 예외 없음 | Safety |
| 9 | approveOrder()가 예외 던지면 에러 메시지 출력 | mock throws `IllegalArgumentException` | 출력에 오류 메시지 포함 | Safety |

---

## 3. ProductionControllerTest

### Regression: 생산 큐 처리
| # | 테스트명 | Mock 조건 | 기대 동작 | 구분 |
|---|---------|---------|---------|------|
| 1 | 생산 큐 처리 실행 — processNext() 호출됨 | mock → CONFIRMED 주문 | `productionService.processNext()` 1회 호출 | Regression |
| 2 | 처리 완료 메시지 출력 | mock → CONFIRMED 주문 | 출력에 "완료" 또는 "CONFIRMED" 포함 | Regression |

### Regression: 생산 큐 조회
| # | 테스트명 | Mock 반환값 | 기대 동작 | 구분 |
|---|---------|-----------|---------|------|
| 3 | 큐 목록 조회 — getQueueSnapshot() 호출됨 | mock `[order1, order2]` | `getQueueSnapshot()` 1회 호출 | Regression |
| 4 | 큐가 비어있을 때 안내 메시지 | mock `[]` | 출력에 "없" 또는 "비어" 포함 | Regression |

### Safety: 빈 큐 처리
| # | 테스트명 | 조건 | 기대 동작 | 구분 |
|---|---------|------|---------|------|
| 5 | processNext()가 IllegalStateException 던지면 에러 메시지 출력 | mock throws `IllegalStateException` | 출력에 오류 메시지 포함, 앱 종료 안 됨 | Safety |

---

## 4. MonitorControllerTest

### Regression: 주문 집계 조회
| # | 테스트명 | Mock 반환값 | 기대 동작 | 구분 |
|---|---------|-----------|---------|------|
| 1 | getOrderCountByStatus() 호출됨 | `{RESERVED:2, CONFIRMED:1, ...}` | `getOrderCountByStatus()` 1회 호출 | Regression |
| 2 | 상태별 주문 수 출력 | mock 반환값 | 출력에 상태명 및 숫자 포함 | Regression |

### Regression: 재고 현황 조회
| # | 테스트명 | Mock 반환값 | 기대 동작 | 구분 |
|---|---------|-----------|---------|------|
| 3 | getStockStatusBySample() 호출됨 | `[info1, info2]` | `getStockStatusBySample()` 1회 호출 | Regression |
| 4 | 시료별 재고 상태 출력 | mock 반환값 | 출력에 시료명, 재고 수, 상태 포함 | Regression |
| 5 | 등록된 시료 없을 때 안내 메시지 | mock `[]` | 출력에 "없" 포함 | Regression |

---

## 5. ReleaseControllerTest

### Regression: 출고 처리
| # | 테스트명 | Mock 조건 / 입력 | 기대 동작 | 구분 |
|---|---------|---------------|---------|------|
| 1 | CONFIRMED 주문 목록 표시 | mock `[order1]` | `orderService.listConfirmedOrders()` 호출됨 | Regression |
| 2 | 주문 선택 후 출고 처리 | 입력 `"O001\n"` | `orderService.releaseOrder("O001")` 호출됨 | Regression |
| 3 | 출고 완료 메시지 출력 | mock → RELEASED 주문 | 출력에 "출고" 또는 "RELEASED" 포함 | Regression |

### Safety: 예외 처리
| # | 테스트명 | 조건 | 기대 동작 | 구분 |
|---|---------|------|---------|------|
| 4 | 출고 대기 주문 없을 때 안내 메시지 | `listConfirmedOrders()` → `[]` | 출력에 "없" 포함, 예외 없음 | Safety |
| 5 | releaseOrder()가 예외 던지면 에러 메시지 | mock throws `IllegalArgumentException` | 출력에 오류 메시지 포함 | Safety |

> **참고:** `OrderService`에 `listConfirmedOrders()`, `releaseOrder(String orderId)` 메서드 추가 필요 (GREEN 단계).

---

## 6. MainControllerTest

### Regression: 메뉴 분기
| # | 테스트명 | 시뮬레이션 입력 | 기대 동작 | 구분 |
|---|---------|--------------|---------|------|
| 1 | 입력 "0" → 루프 종료 | `"0\n"` | run() 정상 종료 | Regression |
| 2 | 입력 "1" → SampleController 서브메뉴 진입 | `"1\n0\n0\n"` | sampleController 의 서브메뉴 메서드 호출됨 | Regression |
| 3 | 입력 "2" → OrderController 서브메뉴 진입 | `"2\n0\n0\n"` | orderController 의 서브메뉴 메서드 호출됨 | Regression |

### Safety: 잘못된 입력
| # | 테스트명 | 시뮬레이션 입력 | 기대 동작 | 구분 |
|---|---------|--------------|---------|------|
| 4 | 범위 밖 숫자 입력 시 재시도 안내 | `"9\n0\n"` | 출력에 오류 메시지, 이후 "0"에 정상 종료 | Safety |
| 5 | 숫자 아닌 문자 입력 시 재시도 안내 | `"abc\n0\n"` | 출력에 오류 메시지, 이후 "0"에 정상 종료 | Safety |

---

## 테스트 파일 위치

```
src/test/java/com/ssemi/sampleorder/
└── controller/
    ├── SampleControllerTest.java    (7개 케이스)
    ├── OrderControllerTest.java     (9개 케이스)
    ├── ProductionControllerTest.java (5개 케이스)
    ├── MonitorControllerTest.java   (5개 케이스)
    ├── ReleaseControllerTest.java   (5개 케이스)
    └── MainControllerTest.java      (5개 케이스)
```

**총 36개 테스트 케이스** — Safety 11개 / Regression 25개

---

## GREEN 단계 구현 대상

### 신규 추가 (OrderService 확장)
- `listConfirmedOrders()` — CONFIRMED 상태 주문 목록 반환
- `releaseOrder(String orderId)` — CONFIRMED → RELEASED 전환 후 저장

### 신규 생성
```
src/main/java/com/ssemi/sampleorder/
├── controller/
│   ├── MainController.java
│   ├── SampleController.java
│   ├── OrderController.java
│   ├── ProductionController.java
│   ├── MonitorController.java
│   └── ReleaseController.java
└── view/
    ├── ConsoleView.java
    ├── MenuView.java
    ├── SampleView.java
    ├── OrderView.java
    ├── ProductionView.java
    ├── MonitorView.java
    └── ReleaseView.java
```
