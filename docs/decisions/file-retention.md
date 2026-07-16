# Re-Echo Decision Records: 파일과 보관 정책

## Decision 011. 파일은 R2 직접 전송과 DB 메타데이터로 분리

### Status

Accepted

### Context

Re-Echo는 파일 첨부와 이미지 미리보기를 제공해야 하며, 파일 바이너리를
애플리케이션 서버가 직접 중계하면 서버 부하와 운영 복잡도가 커질 수
있다.

### Decision

파일 바이너리는 Cloudflare R2에 저장하고, 파일 메타데이터는
PostgreSQL에서 관리한다.
업로드와 다운로드는 Presigned URL 기반 직접 전송으로 처리한다.
파일 첨부는 업로드와 메시지 연결을 분리한다.
프로필 이미지도 동일한 R2 직접 업로드 구조를 사용하되, 이미지 전용
Presigned URL과 파일 메타데이터를 프로필에 연결하는 방식으로 처리한다.

### Reason

PRD, Architecture, ERD, API 문서가 파일 저장소를 외부 오브젝트
스토리지로 두고 Presigned URL 방식으로 직접 업로드/다운로드한다고
명시한다.
API 문서는 이 방식이 대용량 바이너리가 애플리케이션 서버를 통과하지
않게 하기 위한 결정이라고 설명한다.

### Consequences

- 서버는 파일 바이너리를 직접 저장하지 않고 권한 검증, Presigned URL
  발급, 메타데이터 관리에 집중한다.
- 메시지 생성 또는 수정 시 `fileId`가 연결되어야 최종 첨부로 확정된다.
- 프로필 수정 시 `profileImageFileId`가 전달되면 서버가 파일 소유자,
  용도, 업로드 완료 여부를 검증한 뒤 프로필 이미지로 연결한다.
- 업로드만 완료되고 메시지나 프로필 이미지에 연결되지 않은 orphan
  파일은 정기 배치로 정리해야 한다.
- 프로필 이미지 정리 배치는 기본적으로 24시간 이상 참조되지 않은
  파일을 하루 1회 처리하며, 삭제 직전 참조 여부를 다시 확인해야 한다.

### Source

- `docs/prd.md`
- `docs/Architecture.md`
- `docs/ERD.md`
- `docs/API.md`
- `docs/coding-convention.md`

## Decision 012. 워크스페이스와 채널은 보관 후 만료 삭제 모델 사용

### Status

Accepted

### Context

워크스페이스와 채널 삭제는 사용자 협업 데이터와 메시지 접근성에 큰
영향을 주기 때문에 즉시 물리 삭제보다 복원 가능한 상태 전환이 필요하다.

### Decision

워크스페이스와 채널은 즉시 삭제하지 않고 `ARCHIVED` 상태로 전환한다.
보관 후 15일 이내에는 복원 가능하고, 15일 경과 후 자동 삭제한다.
보관된 워크스페이스와 채널은 삭제 전까지 읽기 전용으로 탐색 가능하다.

### Reason

PRD, Architecture, ERD, API 문서가 보관, 복원, 15일 후 자동 삭제,
읽기 전용 탐색 정책을 반복해서 명시한다.

### Consequences

- 삭제 요청은 즉시 물리 삭제가 아니라 상태 전환으로 구현한다.
- `archived_at`, `archive_expires_at`, `deleted_at` 같은 상태 추적
  컬럼이 필요하다.
- 보관 상태에서는 메시지 작성, 수정, 삭제 같은 변경 작업을 제한해야
  한다.
- 정리 배치가 만료된 워크스페이스, 채널, 관련 파일을 처리해야 한다.

### Source

- `docs/prd.md`
- `docs/Architecture.md`
- `docs/ERD.md`
- `docs/API.md`
