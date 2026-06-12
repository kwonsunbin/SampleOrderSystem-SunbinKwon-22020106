# CLAUDE.md

## 1. 프로젝트 맥락 (Project Context)
이 프로젝트는 **"반도체 시료 생산주문관리 시스템"**입니다. 콘솔 기반으로 동작하며, MVC 패턴과 데이터 영속성(CRUD)을 핵심으로 합니다. Claude는 개발 에이전트(Agent)로서 기능 명세를 완벽히 충족하는 고품질의 클린 코드를 작성하고 테스트 프레임워크(Harness)를 도입해야 합니다.

## 2. 개발 및 코딩 규칙 (Coding Guidelines)
- **아키텍처:** MVC 패턴을 엄격히 준수합니다.
    - `Model`: 데이터 구조 및 상태 관리 (Sample, Order, ProductionQueue 등)
    - `View`: 콘솔 입출력 및 UI 화면 Display
    - `Controller`: 사용자 입력을 받아 비즈니스 로직 처리 및 상태 전이 제어
- **상태 관리:** 주문 상태(`RESERVED`, `REJECTED`, `PRODUCING`, `CONFIRMED`, `RELEASED`)의 라이프사이클 흐름도를 엄격히 따르십시오.
- **수율 계산:** 생산라인 구현 시 다음 공식을 반드시 코드로 구현하십시오.
    - `실 생산량 = Math.ceil(부족분 / (수율 * 0.9))`
    - `총 생산 시간 = 평균 생산시간 * 실 생산량`
- **코드 품질:** Clean Code 원칙을 지키고, 함수는 단일 책임 원칙(SRP)을 갖도록 분리하며, 명확한 예외 처리를 포함합니다.

## 3. Git 활용 및 문서화 규칙 (Commit & Documentation)
- **문서 관리:** 코드 변경 시 `PRD.md` 및 시스템 명세와의 정렬 상태를 수시로 확인하십시오.
- **커밋 메시지:** Agentic Engineering 도입 목적에 맞게 변경 사항을 명확히 명시하고 의미 있는 단위로 세분화하여 커밋 이력을 남기십시오. (예: `feat: 구현`, `test: 추가`, `refactor: 리팩토링`)
- **PR 메시지:** Agentic Engineering 도입 목적에 맞게 변경 사항을 명확히 명시하고 의미 있는 단위로 PR 이력을 남기십시오. (예: `feat: 구현`, `test: 추가`, `refactor: 리팩토링`)
- **테스트:** 핵심 도메인 로직(특히 재고 분기 처리 및 수율 계산 계산기)에 대한 단위 테스트(Harness/Test)를 작성하십시오.

# Development Workflow
Always follow ./.skills/agentic-tdd.md.
Always follow ./.skills/test-driven-development.md.

When asked to create a PR:

1. Analyze the git diff.
2. Generate a PR description.
3. Use .github/pull_request_template.md.
4. Fill every section with concrete details.
5. Do not leave placeholders.
