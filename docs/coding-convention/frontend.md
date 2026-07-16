# Re-Echo Coding Convention: 프런트엔드 규칙


### 9.1 State Management

- 서버 상태는 React Query로 관리한다.
- 전역 UI 상태는 store로 관리한다.
- 서버 응답 원본을 store에 중복 저장하지 않는다.

### 9.2 API / WebSocket Access

- 공통 API client와 WebSocket client를 `shared` 계층에 둔다.
- 기능별 query/mutation hook은 `features` 아래에 둔다.
- 인증, base URL, 공통 에러 처리, 재연결 같은 횡단 관심사는 공통
  클라이언트에서 처리한다.
- WebSocket event type과 payload 구조는 `docs/API.md`를 그대로 따른다.

### 9.3 TypeScript Rules

- `props`, `API request/response`, `query result`, `store state` 타입을
  명시한다.
- 타입이 과도하게 복잡해지면 단순한 인터페이스나 타입 별칭을 우선한다.
- 불필요한 범용 제네릭 유틸리티 타입은 피한다.
- API 타입은 서버 계약과 같은 이름 체계를 유지한다.

### 9.4 Styling

- 기본 스타일은 `CSS Module` 중심으로 간다.
- 전역 스타일은 reset, theme token, layout shell 정도로 최소화한다.
- 컴포넌트 스타일 파일은 같은 위치에 colocate한다.
- 인라인 스타일은 동적 값이 꼭 필요한 경우에만 사용한다.

### 9.5 Comment

- Page, React Component, Provider, custom hook, API module, store, 공통 client에는
  화면 또는 모듈의 책임을 설명하는 한국어 한 줄 주석을 선언부 바로 위에
  작성한다.
- 이벤트 handler, mutation callback, `useEffect`, `useMemo`와 같은 내부 로직은
  사용자 흐름, 캐시 동기화, 비동기 경합 방지, 접근성 처리처럼 이름만으로 의도가
  드러나지 않는 경우에만 한국어 한 줄 주석을 작성한다.
- 단순 JSX 조합, 상태 setter 호출, API 함수의 기계적인 위임에는 주석을
  반복하지 않는다. 주석은 UI 구조가 아니라 정책과 상태 전이 이유를 설명한다.

## 10. Database Convention
