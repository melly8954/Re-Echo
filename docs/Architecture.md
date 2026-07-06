# Re-Echo 1차 MVP Architecture

## 1. Architecture Overview

Re-Echo 1차 MVP는 여러 조직이 함께 사용하는 SaaS형 협업 채팅 서비스다.
이번 아키텍처의 목표는 복잡한 확장보다,
`소셜 로그인 -> 워크스페이스 참여 -> 채널 기반 실시간 대화 -> 파일 공유`
흐름을 안정적으로 제공하는 구현 구조를 정의하는 데 있다.

핵심 방향은 다음과 같다.

- 백엔드는 `Java + Spring Boot` 기반 단일 모놀리식으로 시작한다.
- 프론트엔드는 `React + Vite` 기반 `CSR SPA`로 구성한다.
- 프론트엔드와 백엔드는 분리 배포하고, 프론트는 CDN을 통해 정적 파일을 서빙한다.
- 실시간 메시지 송수신과 입력 중 표시는 WebSocket으로 처리한다.
- 영속 데이터의 최종 진실 소스는 PostgreSQL이다.
- Redis는 멀티 인스턴스 확장과 실시간 상태 처리를 위한 보조 저장소로 사용한다.
- 파일 바이너리는 Cloudflare R2에 저장하고, 애플리케이션은 메타데이터와 권한만 관리한다.

## 2. Confirmed Requirements

### 2.1 제품 목표

- 여러 조직이 바로 사용할 수 있는 SaaS형 협업 구조 제공
- 채팅 중심의 실시간 협업 경험 제공
- 파일 공유가 가능한 기본 작업 환경 제공
- 이후 AI, 음성, 화상 기능 확장을 고려할 수 있는 기반 확보

### 2.2 기능 범위

- 소셜 로그인
- 워크스페이스 생성 및 참여
- 역할 기반 멤버 관리
- 공개 채널 및 비공개 채널
- 실시간 메시지
- 파일 첨부
- 채널 단위 읽음 처리
- 보관, 복원, 자동 삭제

### 2.3 제외 범위

- DM
- 메시지 검색
- 멘션
- AI 기능
- 음성 및 화상 협업
- 푸시 알림

### 2.4 확정된 정책

- 워크스페이스 역할은 `OWNER`, `ADMIN`, `MEMBER`로 구분한다.
- 워크스페이스와 채널은 보관 후 15일 뒤 자동 삭제된다.
- 파일은 외부 오브젝트 스토리지에 저장한다.
- 강제 신고, 멤버 신고 같은 기능은 MVP 범위에 포함하지 않는다.

## 3. System Context

시스템 컨텍스트는 다음과 같다.

- 사용자는 CDN에서 서빙되는 React SPA에 접속한다.
- 프론트엔드는 REST API와 WebSocket으로 Spring Boot 백엔드와 통신한다.
- 백엔드는 영속 데이터 저장을 위해 PostgreSQL을 사용한다.
- 백엔드는 분산 상태, Pub/Sub, 토큰 보조 관리를 위해 Redis를 사용한다.
- 파일 바이너리는 Cloudflare R2에 저장하고, 프론트는 Presigned URL로 직접 업로드 및 다운로드한다.
- 소셜 로그인은 외부 OAuth 제공자와 연동한다.

### 3.1 시스템 경계

- 프론트엔드
  - 사용자 UI
  - API 호출
  - WebSocket 연결
  - 파일 직접 업로드 및 다운로드
- 백엔드
  - 인증 및 인가
  - 도메인 규칙 실행
  - 실시간 이벤트 처리
  - 파일 메타데이터 및 보관 정책 관리
- 외부 시스템
  - OAuth Provider
  - Redis
  - PostgreSQL
  - Cloudflare R2

## 4. System Architecture

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
  - 역할, 멤버십, 채널 정책, 메시지, 보관 규칙
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
- Presigned URL 발급
- 읽음 상태 계산
- 보관, 복원, 자동 삭제 정책 수행

### 5.3 Realtime Module

- WebSocket 연결 수립
- 채널 메시지 수신 및 전파
- 입력 중 상태 브로드캐스트
- Redis Pub/Sub 기반 인스턴스 간 이벤트 전달

### 5.4 Scheduler Module

- 보관 만료 데이터 정리
- 고아 파일 정리
- 스토리지 정리 작업

## 6. Data & State Strategy

### 6.1 PostgreSQL

최종 진실 소스로 다음 데이터를 저장한다.

- 사용자 계정
- 워크스페이스
- 워크스페이스 멤버십
- 채널
- 메시지
- 읽음 상태
- 파일 메타데이터
- 강제 삭제 이력

### 6.2 Redis

보조 저장소 및 분산 상태 저장소로 사용한다.

- Access/Refresh Token 보조 관리
- OAuth 로그인 중간 상태
- Pub/Sub
- 입력 중 상태
- 인스턴스 간 공유 상태
- 일부 보조 운영 데이터

원칙:

- 최종 진실 소스는 PostgreSQL이다.
- Redis는 캐시, 보조 상태, 분산 이벤트 전파 역할만 맡는다.

### 6.3 단일 RDB 원칙

- 사용자 권한 데이터와 메시지 데이터는 하나의 PostgreSQL에서 함께 운영한다.
- MVP 단계에서는 DB 분리보다 단일 운영의 단순성을 우선한다.

## 7. Real-time / Async Strategy

### 7.1 실시간 처리 범위

- 메시지 송수신
- 입력 중 표시

### 7.2 WebSocket 인증

- Access Token 기반 handshake 인증

### 7.3 멀티 인스턴스 이벤트 전파

- Redis Pub/Sub 사용
- WebSocket 연결과 일반 HTTP 요청은 같은 백엔드 애플리케이션에서 처리한다.
- Sticky session은 필수 전제로 두지 않는다.

### 7.4 Redis 장애 시 완화 원칙

- 실시간 기능은 일시적으로 저하될 수 있다.
- 일반 API와 영속 데이터 처리는 최대한 유지한다.

## 8. File & Storage Strategy

### 8.1 저장 구조

- 실제 파일 바이너리: Cloudflare R2
- 파일 메타데이터: PostgreSQL

### 8.2 연동 구조

- 스토리지 인터페이스를 두고 R2 어댑터로 구현한다.
- 업로드 및 다운로드는 Presigned URL 기반 직접 전송으로 처리한다.

### 8.3 정리 정책

- 메타데이터 등록 실패 시 고아 파일 정리 정책이 필요하다.
- 보관 만료 삭제 시 스토리지 정리도 함께 수행한다.

## 9. Authentication & Authorization Strategy

### 9.1 로그인

- Google
- Kakao
- GitHub

### 9.2 인증 구조

- Access Token + Refresh Token 기반
- Access Token
  - 브라우저 메모리 보관
- Refresh Token
  - HttpOnly cookie 기반
- OAuth 로그인 중간 상태
  - Redis 공유 저장소 사용

### 9.3 인가 구조

- 워크스페이스 역할 기반 권한 제어
- 채널 접근은 채널 멤버십과 공개 여부로 추가 검증

## 10. External Integrations

### 10.1 OAuth Providers

- 소셜 로그인 처리
- 멀티 인스턴스 환경에서 로그인 중간 상태 공유 필요

### 10.2 Redis

- Pub/Sub
- 토큰 보조 관리
- 공유 상태 관리

### 10.3 Cloudflare R2

- 첨부 파일 저장
- 다운로드 및 업로드 Presigned URL 발급 대상

## 11. Deployment Architecture

배포 구조는 다음과 같다.

- 프론트엔드는 정적 파일을 CDN으로 서빙한다.
- 백엔드 요청은 Reverse Proxy 또는 Load Balancer를 통해 Spring Boot 애플리케이션으로 전달한다.
- 백엔드는 PostgreSQL, Redis, Cloudflare R2와 연동한다.

### 11.1 초기 운영

- 앱 인스턴스 1개로 시작
- 구조는 멀티 인스턴스 확장이 가능하도록 설계
- 로컬 개발 또는 단순 운영 환경에서는 PostgreSQL과 Redis를 docker-compose로 띄울 수 있다.

### 11.2 확장 원칙

- 백엔드는 수평 확장 가능
- Redis Pub/Sub 및 공유 상태로 멀티 인스턴스를 지원

## 12. Technology Decisions

- Backend
  - Java
  - Spring Boot
- Frontend
  - React
  - Vite
- Rendering
  - CSR SPA
- RDB
  - PostgreSQL
- Shared State / Realtime Support
  - Redis
- Object Storage
  - Cloudflare R2
- Scheduler
  - 애플리케이션 내부 스케줄러

## 13. Non-functional Requirements

### 13.1 우선순위

1. 실시간 안정성과 장애 복구 용이성
2. 운영 단순성과 개발 속도

### 13.2 관측성

- 애플리케이션 로그
- 메트릭
- 에러 추적
- 기본 대시보드

예상 구성:

- Spring Boot Actuator
- Micrometer
- Prometheus
- Grafana
- Sentry 또는 동급 에러 추적 도구

### 13.3 운영 원칙

- Redis 장애 시 실시간 기능 저하는 허용
- 영속 API는 가능한 한 유지
- 정기 작업은 내부 스케줄러로 단순하게 운영

## 14. Design Decisions

- 단일 모놀리식 백엔드로 시작
- 프론트엔드와 백엔드는 분리 배포
- 프론트는 CSR SPA
- Access/Refresh Token 기반 인증
- OAuth 임시 상태는 Redis에 저장
- WebSocket은 Access Token handshake 사용
- Redis Pub/Sub로 멀티 인스턴스 실시간 이벤트 전파
- PostgreSQL을 최종 진실 소스로 유지
- Redis는 보조 저장소 및 분산 상태 계층으로 사용
- 파일 저장소는 스토리지 인터페이스 + R2 어댑터 구조로 구성
- 정리 배치는 내부 스케줄러 사용
