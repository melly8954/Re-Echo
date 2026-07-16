# Re-Echo 1차 MVP Architecture: 파일, 인증, 외부 연동


### 8.1 저장 구조

- 실제 파일 바이너리: Cloudflare R2
- 파일 메타데이터: PostgreSQL
- 프로필 이미지도 R2 바이너리와 DB 메타데이터를 분리해 저장한다.
  OAuth Provider가 제공한 외부 이미지 URL은 사용자가 직접 업로드한
  R2 이미지로 교체하기 전까지 기본 프로필 이미지로 사용할 수 있다.

### 8.2 연동 구조

- 스토리지 인터페이스를 두고 R2 어댑터로 구현한다.
- 업로드 및 다운로드는 Presigned URL 기반 직접 전송으로 처리한다.
- 프로필과 워크스페이스 대표 이미지 업로드는 이미지 전용 Presigned URL을
  발급하고, 수정 시 서버가 파일 소유자, 용도, 업로드 완료 여부를 검증한 뒤
  각 대상에 연결한다.

### 8.3 정리 정책

- 메타데이터 등록 실패 시 고아 파일 정리 정책이 필요하다.
- 메시지 첨부, 프로필 이미지 또는 워크스페이스 대표 이미지에 최종 연결되지 않은 파일은 고아
  파일로 보고 지연 정리한다.
- 프로필 이미지 정리 배치는 기본적으로 24시간 이상 참조되지 않은
  `PROFILE_IMAGE` 파일을 최대 100개씩 처리한다.
- 정리 대상 파일은 삭제 직전 사용자 계정과 워크스페이스 멤버십의
  프로필 이미지 참조 여부를 다시 확인한다.
- 보관 만료 삭제 시 스토리지 정리도 함께 수행한다.

## 9. Authentication & Authorization Strategy

### 9.1 로그인

- Google
- Kakao
- GitHub

### 9.2 인증 구조

- Access Token + Refresh Token 기반
- OAuth 로그인
  - Spring Security `oauth2Login` 기반 리다이렉트 흐름
  - 백엔드가 OAuth 시작, 콜백, 인가 코드 교환, 사용자 정보 조회를 담당
- Access Token
  - 브라우저 메모리 보관
- Refresh Token
  - HttpOnly cookie 기반
  - 세션별 `jti`를 Redis에 저장하고 재발급 시 원자적으로 교체
- OAuth 로그인 중간 상태
  - Spring Session Redis 공유 저장소 사용

### 9.3 인가 구조

- 워크스페이스 역할 기반 권한 제어
- 워크스페이스 멤버십은 역할뿐 아니라 워크스페이스별 프로필의 기준이 된다.
- 채널 접근은 채널 멤버십과 공개 여부로 추가 검증

## 10. External Integrations

### 10.1 OAuth Providers

- Spring Security OAuth2 Client 기반 소셜 로그인 처리
- 백엔드가 OAuth Provider와 직접 연동해 인가 코드 교환과 사용자 정보
  조회를 수행
- 멀티 인스턴스 환경에서 로그인 중간 상태 공유 필요

### 10.2 Redis

- Pub/Sub
- 토큰 보조 관리
- 공유 상태 관리

### 10.3 Cloudflare R2

- 첨부 파일 저장
- 프로필 이미지 저장
- 다운로드 및 업로드 Presigned URL 발급 대상
