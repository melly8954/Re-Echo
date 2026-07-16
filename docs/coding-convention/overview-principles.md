# Re-Echo Coding Convention: 개요와 공통 원칙

## 1. Coding Convention Overview

이 문서는 Re-Echo MVP 구현 시 코드 구조, 계층 책임, 네이밍,
예외 처리, 테스트, Git 규칙을 일관되게 유지하기 위한 기준이다.

이 문서는 기술 선택을 새로 결정하는 문서가 아니다.
이미 확정된 PRD, Architecture, ERD, API 문서를 구현 코드로
안전하게 옮기기 위한 규칙만 정의한다.

## 2. Source Documents

- `docs/prd.md`
- `docs/Architecture.md`
- `docs/ERD.md`
- `docs/API.md`

참고:

- `docs/API.md`는 클라이언트와 서버의 계약 문서다.
- Endpoint, Request/Response DTO, 공통 응답 형식, 에러 코드,
  인증 방식, 실시간 이벤트 형식은 `docs/API.md`를 우선 따른다.
- 이 문서는 API 구현 방식과 코드 배치 원칙만 보완한다.

## 3. Document Priority

문서 간 충돌 시 우선순위는 다음과 같다.

1. `docs/prd.md`
2. `docs/Architecture.md`
3. `docs/ERD.md`
4. `docs/API.md`
5. `docs/coding-convention.md`

원칙:

- 제품 정책과 범위는 PRD를 따른다.
- 시스템 구조와 기술 선택은 Architecture를 따른다.
- 데이터 구조와 상태 값은 ERD를 따른다.
- API 계약은 API 문서를 따른다.
- Coding Convention은 상위 문서를 구현 코드로 옮기는 규칙만
  정의한다.

## 4. Common Principles

- 최소한의 추상화로 시작한다.
- 구조가 명확해지기 전까지 과도한 공통화는 하지 않는다.
- Architecture에 없는 패턴을 임의로 도입하지 않는다.
- Controller, Service, Repository, Entity, DTO의 책임을 섞지
  않는다.
- DTO와 Entity는 항상 분리한다.
- 서버 상태와 UI 상태는 분리한다.
- 읽기와 쓰기 유스케이스는 CQRS 관점에서 분리한다.
- 구현 편의보다 유지보수성과 계약 일관성을 우선한다.
- API 응답 형식은 구현 편의로 바꾸지 않고 `docs/API.md`에 맞춘다.

## 5. Naming Convention
