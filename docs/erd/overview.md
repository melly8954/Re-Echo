# Re-Echo 1차 MVP ERD: 개요와 도메인 모델

## 1. ERD Overview

Re-Echo 1차 MVP ERD는 다음 흐름을 안정적으로 지원하는 것을 목표로 한다.

- 소셜 로그인
- 계정 기본 프로필
- 워크스페이스 생성 및 참여
- 역할 기반 멤버 관리
- 워크스페이스별 프로필 관리
- 공개 채널 및 비공개 채널 운영
- 실시간 메시지 송수신
- 파일 첨부
- 채널 단위 읽음 상태 추적
- 보관, 복원, 자동 삭제

이 ERD는 `docs/prd.md`와 `docs/Architecture.md`를 기준으로 작성한다.
MVP 범위 밖인 DM, 메시지 검색, 멘션, AI 기능은 포함하지 않는다.

## 2. Confirmed Data Requirements

### 2.1 확정된 기능 범위

- 사용자 계정은 Google, Kakao, GitHub 소셜 로그인으로 생성된다.
- 사용자는 여러 워크스페이스를 생성하거나 참여할 수 있다.
- 워크스페이스마다 `OWNER`, `ADMIN`, `MEMBER` 역할이 있다.
- 워크스페이스별 닉네임과 프로필 이미지를 관리한다.
- 워크스페이스 참여 시 기본 채널 `#general`에 자동 참여한다.
- 채널은 공개 채널과 비공개 채널로 구분된다.
- 메시지는 실시간으로 송수신되며 본인 메시지는 수정할 수 있다.
- 관리자는 모든 멤버 메시지를 삭제할 수 있다.
- 파일 첨부 메타데이터는 애플리케이션 DB에서 관리한다.
- 읽음 상태는 마지막 읽은 메시지 기준으로 채널 단위 계산을 사용한다.
- 워크스페이스와 채널은 보관 후 15일 뒤 자동 삭제된다.

### 2.2 확정된 제외 범위

- DM
- 메시지 검색
- 멘션
- 푸시 알림
- AI, 음성, 화상 관련 기능

### 2.3 확정된 데이터 운영 정책

- 최종 진실 소스는 PostgreSQL이다.
- Redis는 실시간 상태와 보조 운영 데이터 저장소이며 ERD 범위에 포함하지 않는다.
- 파일 바이너리는 Cloudflare R2에 저장하고 DB에는 메타데이터만 저장한다.
- 강제 신고, 멤버 신고 같은 제재 도메인은 없다.
- 워크스페이스와 채널은 즉시 삭제하지 않고 보관 상태를 거친다.

## 3. Data Storage Context

### 3.1 영속 저장소

- PostgreSQL
  - 사용자, 워크스페이스, 멤버십, 채널, 메시지, 읽음 상태, 파일 메타데이터 저장

### 3.2 비영속 또는 보조 저장소

- Redis
  - OAuth 로그인 중간 상태
  - 토큰 보조 관리
  - 입력 중 상태
  - Pub/Sub

### 3.3 외부 저장소

- Cloudflare R2
  - 파일 바이너리 저장

## 4. Core Domains

### 4.1 사용자 및 인증 도메인

- 사용자 계정 식별
- 소셜 로그인 공급자 계정 연결 관리
- 워크스페이스가 없는 상태와 새 멤버십 초기화에 사용할 계정 기본
  프로필 관리

### 4.2 워크스페이스 및 멤버십 도메인

- 워크스페이스 생성, 보관, 복원, 삭제
- 워크스페이스 대표 이미지 관리
- 워크스페이스별 멤버 역할과 상태 관리
- 워크스페이스별 닉네임과 프로필 이미지 관리
- 초대 링크 기반 참여 관리

### 4.3 채널 도메인

- 공개 채널 및 비공개 채널 관리
- 채널 참여와 접근 제어
- `#general` 기본 채널 보장

### 4.4 메시지 및 읽음 도메인

- 메시지 생성, 수정, 삭제
- 채널 단위 읽음 위치 관리
- 안읽은 메시지 수 계산용 기준 제공

### 4.5 파일 도메인

- 파일 메타데이터 저장
- 메시지 첨부 연결
- 고아 파일 정리 및 보관 만료 정리 지원

## 5. Domain Objects

- User
  - Re-Echo 사용자 계정
- UserIdentity
  - 소셜 로그인 공급자와 사용자 계정 연결
- Workspace
  - 최상위 협업 공간
- WorkspaceMembership
  - 사용자 워크스페이스 소속, 역할, 상태, 워크스페이스별 프로필
- WorkspaceInviteLink
  - 워크스페이스 초대 링크
- Channel
  - 워크스페이스 내부 대화 공간
- ChannelMembership
  - 사용자 채널 참여 상태
- Message
  - 채널 메시지
- ChannelReadState
  - 사용자별 채널 읽음 기준점
- FileObject
  - 외부 스토리지 파일 메타데이터
- MessageAttachment
  - 메시지와 파일 연결

## 6. Entity List

- `users`
- `user_identities`
- `workspaces`
- `workspace_memberships`
- `workspace_invite_links`
- `channels`
- `channel_memberships`
- `messages`
- `channel_read_states`
- `file_objects`
- `message_attachments`

## 7. Entity Details
