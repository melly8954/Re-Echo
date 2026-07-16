# Re-Echo Decision Records: 실시간과 메시지

## Decision 008. 실시간 처리는 메시지와 입력 중 표시로 제한

### Status

Accepted

### Context

1차 MVP는 채널 기반 실시간 협업 경험을 제공해야 하지만, 모든 상태를
실시간 이벤트로 전파할 필요는 없다.

### Decision

WebSocket 실시간 처리는 메시지 송수신과 입력 중 표시로 제한한다.
이벤트 타입은 `MESSAGE_CREATED`, `MESSAGE_UPDATED`, `MESSAGE_DELETED`,
`TYPING_UPDATED`만 사용한다.
메시지 이벤트와 typing 이벤트는 부분 변경이 아니라 snapshot 형태로
전달한다.

### Reason

PRD와 API 문서가 실시간 이벤트 타입과 payload 정책을 확정한다.
Architecture는 실시간 처리 범위를 메시지 송수신과 입력 중 표시로
제한한다.

### Consequences

- 읽음 상태, 멤버 목록, 채널 목록 같은 다른 상태를 임의로 WebSocket
  이벤트에 추가하지 않는다.
- 클라이언트는 `eventId`로 중복 이벤트를 제거한다.
- 메시지 최신성 판단은 `occurredAt`이 아니라
  `payload.message.updatedAt`을 기준으로 한다.

### Source

- `docs/prd.md`
- `docs/Architecture.md`
- `docs/API.md`
- `docs/coding-convention.md`

## Decision 009. 메시지 목록은 cursor pagination으로 조회

### Status

Accepted

### Context

채팅 화면은 최신 메시지 기준으로 진입하고, 과거 메시지는 위로 스크롤하며
로드해야 한다.

### Decision

메시지 목록은 cursor pagination을 사용한다.
cursor 기준값은 `createdAt + messageId` 조합으로 두고, cursor는 opaque
string이 아니라 객체 형태로 노출한다.
응답 page 메타데이터는 더 과거 메시지를 조회하기 위한 `nextCursor`를
제공한다.

### Reason

PRD와 API 문서가 메시지 목록의 cursor pagination과 cursor 기준값을
명시한다.
Coding Convention은 메시지 목록 cursor 구조를 API 문서 기준 그대로
따르도록 한다.

### Consequences

- 메시지 조회 API와 구현 DTO는 `createdAt`, `messageId` 기준 cursor
  구조를 유지해야 한다.
- offset pagination을 메시지 목록의 기본 방식으로 바꾸지 않는다.
- 동일 시각 메시지의 안정적 정렬을 위해 `messageId`를 함께 사용한다.

### Source

- `docs/prd.md`
- `docs/API.md`
- `docs/coding-convention.md`

## Decision 010. 읽음 상태는 채널별 마지막 읽은 메시지 기준으로 관리

### Status

Accepted

### Context

1차 MVP는 채널 단위 안읽음 배지와 읽지 않은 메시지 개수를 제공해야
하지만, 메시지별 읽음 상세 표시는 후속 확장 후보에 가깝다.

### Decision

읽음 상태는 메시지별 영수증 테이블이 아니라 채널별 마지막 읽은 메시지
기준점으로 관리한다.
읽음 갱신은 REST API로만 처리하고 별도 read broadcast는 제공하지 않는다.

### Reason

PRD와 API 문서는 읽음 갱신을 REST API로만 처리하고 별도 broadcast를
제공하지 않는다고 명시한다.
ERD와 API 문서는 읽음 상태를 채널별 마지막 읽은 메시지 기준으로
단순화한다고 명시한다.

### Consequences

- 안읽음 계산은 `channel_read_states`의 마지막 읽은 메시지 기준으로
  수행한다.
- 메시지별 사용자 읽음 영수증 테이블은 MVP ERD에 포함하지 않는다.
- WebSocket 이벤트에 읽음 상태 전용 이벤트를 임의로 추가하지 않는다.

### Source

- `docs/prd.md`
- `docs/ERD.md`
- `docs/API.md`
- `docs/coding-convention.md`
