# Phase 2 테스트 계획 (RED Phase)

## 대상 클래스
- `SampleRepository` (인터페이스)
- `OrderRepository` (인터페이스)
- `JsonSampleRepository` (JSON 파일 기반 구현체)
- `JsonOrderRepository` (JSON 파일 기반 구현체)

---

## 설계 방향

- 테스트 실행 시 `data/test/` 임시 디렉터리를 사용하고, 각 테스트 후 파일 삭제 (`@AfterEach`)
- 실제 파일 I/O를 수행하는 통합 테스트 방식 (mock 없음) — PRD의 영속성 요건을 직접 검증
- Repository는 인터페이스로 추상화, 테스트는 구현체(`Json*`)를 대상으로 작성

---

## 1. JsonSampleRepositoryTest

### Round-trip 직렬화 (Regression)
| # | 테스트명 | 입력 | 기대 결과 | 구분 |
|---|---------|------|---------|------|
| 1 | save 후 findById로 동일 객체 조회 | `save(sample)` → `findById(id)` | 모든 필드 일치 | Regression |
| 2 | save 후 findAll에 포함됨 | `save(sample)` → `findAll()` | 목록에 해당 시료 포함 | Regression |
| 3 | 복수 save 후 findAll 전체 반환 | `save` 3건 → `findAll()` | size == 3, 모두 포함 | Regression |
| 4 | findAll 반환 순서는 저장 순서와 일치 | 순서대로 save 3건 → `findAll()` | 저장 순서 동일 | Regression |
| 5 | save 후 재시작 시뮬레이션 — 새 인스턴스로 findById | 동일 파일 경로로 새 Repository 생성 후 조회 | 필드 값 동일 (영속성 검증) | Regression |
| 6 | update: 동일 id로 save 시 덮어쓰기 | `save(v1)` → `save(v2, 같은 id)` → `findById` | v2 필드 반환 | Regression |
| 7 | stock 변경 후 save → 재로드 시 변경된 stock 영속화 | `addStock(10)` → `save` → 새 인스턴스 `findById` | 변경된 stock 반환 | Regression |

### 조회 및 검색 (Regression)
| # | 테스트명 | 입력 | 기대 결과 | 구분 |
|---|---------|------|---------|------|
| 8 | findByName 정확 일치 검색 | `name="GaAs"` → `findByName("GaAs")` | 해당 시료 반환 | Regression |
| 9 | findByName 부분 일치 검색 | `name="GaAs 웨이퍼"` → `findByName("웨이퍼")` | 해당 시료 포함 | Regression |
| 10 | findByName 대소문자 무관 검색 | `name="GaAs"` → `findByName("gaas")` | 해당 시료 반환 | Regression |

### 삭제 (Regression)
| # | 테스트명 | 입력 | 기대 결과 | 구분 |
|---|---------|------|---------|------|
| 11 | delete 후 findById 빈 결과 | `save` → `delete(id)` → `findById` | `Optional.empty()` | Regression |
| 12 | delete 후 findAll에서 제거됨 | `save` 2건 → `delete` 1건 → `findAll` | size == 1 | Regression |

### 경계값 / 예외 — null 입력 (Safety)
| # | 테스트명 | 입력 | 기대 결과 | 구분 |
|---|---------|------|---------|------|
| 13 | save(null) → IllegalArgumentException | `save(null)` | `IllegalArgumentException` | Safety |
| 14 | findById(null) → IllegalArgumentException | `findById(null)` | `IllegalArgumentException` | Safety |
| 15 | delete(null) → IllegalArgumentException | `delete(null)` | `IllegalArgumentException` | Safety |
| 16 | findByName(null) → IllegalArgumentException | `findByName(null)` | `IllegalArgumentException` | Safety |

### 경계값 / 예외 — 파일/디렉터리 부재 (Safety)
| # | 테스트명 | 입력 | 기대 결과 | 구분 |
|---|---------|------|---------|------|
| 17 | 파일 없을 때 findAll → 빈 목록 (첫 실행 안전성) | 파일 미존재 상태에서 `findAll()` | 빈 List, 예외 없음 | Safety |
| 18 | 존재하지 않는 id findById → Optional.empty() | `findById("없는ID")` | `Optional.empty()` | Safety |
| 19 | 존재하지 않는 id delete → 조용히 무시 | `delete("없는ID")` | 예외 없음 | Safety |
| 20 | findByName 결과 없음 → 빈 목록 | `findByName("없는이름")` | 빈 List 반환 | Safety |
| 21 | data 디렉터리 미존재 시 save → 디렉터리 자동 생성 후 성공 | 디렉터리 없는 경로로 `save` | 예외 없이 저장됨 | Safety |

---

## 2. JsonOrderRepositoryTest

### Round-trip 직렬화 (Regression)
| # | 테스트명 | 입력 | 기대 결과 | 구분 |
|---|---------|------|---------|------|
| 1 | save 후 findById로 동일 객체 조회 | `save(order)` → `findById(id)` | 모든 필드 일치 (status 포함) | Regression |
| 2 | save 후 findAll에 포함됨 | `save(order)` → `findAll()` | 목록에 해당 주문 포함 | Regression |
| 3 | 복수 save 후 findAll 전체 반환 | `save` 3건 → `findAll()` | size == 3 | Regression |
| 4 | findAll 반환 순서는 저장 순서와 일치 | 순서대로 save 3건 → `findAll()` | 저장 순서 동일 | Regression |
| 5 | save 후 재시작 시뮬레이션 | 새 Repository 인스턴스로 findById | status, quantity 등 필드 동일 | Regression |
| 6 | 상태 변경 후 save → 변경된 status 영속화 | `transitionTo(CONFIRMED)` → `save` → 새 인스턴스 `findById` | status == CONFIRMED | Regression |

### 상태별 필터 조회 (Regression)
| # | 테스트명 | 입력 | 기대 결과 | 구분 |
|---|---------|------|---------|------|
| 7 | findByStatus(RESERVED) — 해당 상태만 반환 | RESERVED 2건, CONFIRMED 1건 저장 | size == 2, 모두 RESERVED | Regression |
| 8 | findByStatus(CONFIRMED) — 정확히 필터링 | RESERVED 1건, CONFIRMED 2건 저장 | size == 2, 모두 CONFIRMED | Regression |
| 9 | 혼합 상태(RESERVED/CONFIRMED/PRODUCING) 저장 후 각 findByStatus 카운트 정확성 | 각 1건씩 저장 | 각 status별 size == 1 | Regression |
| 10 | findByStatus 결과 없으면 빈 목록 | RESERVED만 저장 후 `findByStatus(RELEASED)` | 빈 List 반환 | Regression |

### 삭제 (Regression)
| # | 테스트명 | 입력 | 기대 결과 | 구분 |
|---|---------|------|---------|------|
| 11 | delete 후 findById 빈 결과 | `save` → `delete(id)` → `findById` | `Optional.empty()` | Regression |
| 12 | delete 후 findAll에서 제거됨 | `save` 3건 → `delete` 1건 → `findAll` | size == 2 | Regression |

### 경계값 / 예외 — null 입력 (Safety)
| # | 테스트명 | 입력 | 기대 결과 | 구분 |
|---|---------|------|---------|------|
| 13 | save(null) → IllegalArgumentException | `save(null)` | `IllegalArgumentException` | Safety |
| 14 | findById(null) → IllegalArgumentException | `findById(null)` | `IllegalArgumentException` | Safety |
| 15 | delete(null) → IllegalArgumentException | `delete(null)` | `IllegalArgumentException` | Safety |
| 16 | findByStatus(null) → IllegalArgumentException | `findByStatus(null)` | `IllegalArgumentException` | Safety |

### 경계값 / 예외 — 파일/디렉터리 부재 (Safety)
| # | 테스트명 | 입력 | 기대 결과 | 구분 |
|---|---------|------|---------|------|
| 17 | 파일 없을 때 findAll → 빈 목록 | 파일 미존재 상태에서 `findAll()` | 빈 List, 예외 없음 | Safety |
| 18 | 존재하지 않는 id findById → Optional.empty() | `findById("없는ID")` | `Optional.empty()` | Safety |
| 19 | 존재하지 않는 id delete → 조용히 무시 | `delete("없는ID")` | 예외 없음 | Safety |
| 20 | createdAt 직렬화 — LocalDateTime 왕복 정확성 | save 후 새 인스턴스 findById | createdAt 동일 (초 단위) | Safety |
| 21 | data 디렉터리 미존재 시 save → 디렉터리 자동 생성 후 성공 | 디렉터리 없는 경로로 `save` | 예외 없이 저장됨 | Safety |

---

## 테스트 파일 위치

```
src/test/java/com/ssemi/sampleorder/
└── repository/
    ├── JsonSampleRepositoryTest.java   (21개 케이스)
    └── JsonOrderRepositoryTest.java    (21개 케이스)
```

**총 42개 테스트 케이스** — Safety 18개 / Regression 24개

---

## 생성될 프로덕션 클래스 (GREEN Phase 대상)

```
src/main/java/com/ssemi/sampleorder/
├── repository/
│   ├── SampleRepository.java           — CRUD 인터페이스
│   ├── OrderRepository.java            — CRUD 인터페이스
│   └── json/
│       ├── JsonSampleRepository.java   — JSON 파일 구현체
│       └── JsonOrderRepository.java    — JSON 파일 구현체
└── repository/util/
    └── JsonFileUtil.java               — JSON 직렬화/역직렬화 유틸
```

## JSON 직렬화 방식

- `build.gradle.kts`에 `org.json` 의존성 추가
- 파일 경로: `data/samples.json`, `data/orders.json` (테스트 시 `data/test/`)
- `data/` 디렉터리 미존재 시 save 시점에 자동 생성
