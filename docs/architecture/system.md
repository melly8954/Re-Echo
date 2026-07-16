# Re-Echo 1차 MVP Architecture: 시스템 구조와 구성 요소


### 4.1 전체 구조

- `단일 모놀리식 백엔드`
- `분리 배포형 CSR SPA 프론트엔드`
- `초기 단일 인스턴스 배포`
- `멀티 인스턴스 확장 가능 구조`

### 4.2 계층 구조

- Entry 계층
  - REST API
  - WebSocket handshake 및 event handling
- Application 계층
  - 유스케이스 조합
  - 트랜잭션 경계
  - 권한 검증 호출
- Domain 계층
  - 역할, 멤버십, 워크스페이스별 프로필, 채널 정책, 메시지, 보관 규칙
- Infrastructure 계층
  - PostgreSQL, Redis, OAuth, R2, 스케줄러 연동

## 5. System Components

### 5.1 Frontend

- `React + Vite`
- `CSR SPA`
- CDN 정적 배포
- Access Token은 브라우저 메모리에 보관
- Refresh Token은 HttpOnly cookie 기반으로 사용

### 5.2 Backend API

- 인증 및 인가 처리
- 워크스페이스, 초대, 멤버십, 채널, 메시지 유스케이스 제공
- 워크스페이스별 닉네임 및 프로필 이미지, 워크스페이스 대표 이미지 관리
- Presigned URL 발급
- 읽음 상태 계산
- 보관, 복원, 자동 삭제 정책 수행
- 보관된 워크스페이스와 채널의 읽기 전용 탐색 지원

### 5.3 Realtime Module

- WebSocket 연결 수립
- 채널 메시지 수신 및 전파
- 입력 중 상태 브로드캐스트
- Redis Pub/Sub 기반 인스턴스 간 이벤트 전달

### 5.4 Scheduler Module

- 보관 만료 데이터 정리
- 고아 파일 정리
- 스토리지 정리 작업
- 파일 정리 작업은 애플리케이션 내부 스케줄러로 실행하며, 기본값은
  하루 1회 새벽 3시 실행이다.

## 6. Data & State Strategy
