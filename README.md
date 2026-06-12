# 반도체 시료 생산주문관리 시스템

S-Semi의 반도체 시료(Sample) 생산·주문을 콘솔 기반으로 관리하는 시스템입니다.  
MVC 패턴과 JSON 파일 기반 영속성을 사용합니다.

---

## 실행 방법

### 1. 영속 모드 (기본) — 이전 데이터 유지

```bash
./gradlew run
```

`data/samples.json`, `data/orders.json`에 저장된 기존 데이터를 그대로 불러와 시작합니다.  
앱 종료 후에도 변경 사항이 파일에 저장되어 다음 실행 시 이어집니다.

### 2. 클린 모드 — 데이터 초기화 후 시작

```bash
./gradlew runClean
```

기존 데이터 파일을 삭제하고 빈 상태로 시작합니다.  
처음부터 새로 테스트하고 싶을 때 사용합니다.

### 3. 직접 JAR 실행

```bash
# 영속 모드
java -jar build/libs/SampleOrderSystem-1.0-SNAPSHOT.jar

# 클린 모드
java -jar build/libs/SampleOrderSystem-1.0-SNAPSHOT.jar --clean
```

---

## 데이터 파일 위치

| 파일 | 내용 |
|------|------|
| `data/samples.json` | 등록된 시료 목록 및 재고 |
| `data/orders.json`  | 주문 이력 및 상태 |

수동으로 데이터를 수정하려면 앱 종료 후 해당 파일을 직접 편집하면 됩니다.

---

## 테스트 실행

```bash
./gradlew test
```

---

## 주문 상태 흐름

```
RESERVED → (승인) → CONFIRMED / PRODUCING
         → (거절) → REJECTED

PRODUCING → (생산완료) → CONFIRMED
CONFIRMED → (출고) → RELEASED
```

---

## 프로젝트 구조

```
src/main/java/com/ssemi/sampleorder/
├── Main.java               # 진입점 (--clean 플래그 처리)
├── model/                  # 도메인 모델 (Sample, Order, ProductionQueue)
├── repository/             # 데이터 접근 계층 (JSON 파일 기반)
├── service/                # 비즈니스 로직
├── controller/             # 사용자 입력 처리 및 상태 전이
└── view/                   # 콘솔 UI
data/                       # 영속성 데이터 (JSON)
docs/                       # 설계 문서 (PRD, ARCHITECTURE 등)
```

---

자세한 기능 명세는 [docs/PRD.md](docs/PRD.md)를, 아키텍처 설명은 [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)를 참고하세요.
