# Re-Echo Coding Convention: 네이밍과 프로젝트 구조


### 5.1 Common Rules

- 이름은 역할이 드러나야 한다.
- 축약어는 팀에서 자주 쓰는 경우만 허용한다.
- 모호한 짧은 이름보다 긴 명확한 이름을 우선한다.

### 5.2 Backend Naming

- Controller: `WorkspaceController`
- Command Service: `CreateWorkspaceCommandService`
- Query Service: `WorkspaceQueryService`
- Repository: `WorkspaceRepository`
- Entity: `Workspace`, `Channel`, `Message`
- Mapper: `WorkspaceMapper`
- Request DTO: `CreateWorkspaceRequest`
- Response DTO: `WorkspaceResponse`, `ChannelMessageResponse`
- Exception: `WorkspaceArchivedException`, `InviteLinkExpiredException`

원칙:

- Service는 `CommandService`, `QueryService` suffix를 명확히 쓴다.
- DTO는 `Request`, `Response`로 구분한다.
- Java DTO는 특별한 이유가 없으면 `record`로 작성한다.
- 범용 `Dto`, `Vo`, `Util` 같은 이름은 피한다.
- API 문서의 리소스 이름과 코드의 DTO 이름이 크게 어긋나지 않게
  유지한다.

### 5.3 Frontend Naming

- React 컴포넌트: `PascalCase`
- hooks: `useCamelCase`
- store: `useXxxStore` 또는 `xxxStore`
- CSS Module: 컴포넌트와 같은 이름 + `.module.css`

예:

- `MessageInput.tsx`
- `MessageInput.module.css`

### 5.4 Database Naming

- 테이블명: `snake_case` 복수형
- 컬럼명: `snake_case`
- FK 컬럼명은 참조 대상을 포함한다.

예:

- `workspace_memberships`
- `channel_read_states`
- `workspace_id`
- `created_by_user_id`

## 6. Project Structure Convention

### 6.1 Backend Structure

백엔드는 `도메인 중심 + 공통 계층 분리` 구조를 따른다.

예시:

```text
backend
├─ common
├─ config
├─ security
├─ infra
├─ auth
├─ workspace
├─ channel
├─ message
└─ file
```

도메인 패키지 예시:

```text
workspace
├─ controller
├─ service
│  ├─ command
│  └─ query
├─ dto
├─ domain
├─ repository
├─ mapper
└─ exception
```

원칙:

- 도메인 패키지 안에 해당 도메인의 Controller, Service, DTO,
  Repository를 함께 둔다.
- `config`, `security`, `common`, `infra`는 별도 상위 패키지로 둔다.
- 여러 도메인에서 실제로 공유되는 코드만 `common`으로 올린다.

### 6.2 Frontend Structure

프론트는 `pages / features / components / shared / stores` 구조를 따른다.

예시:

```text
src
├─ pages
├─ features
│  ├─ auth
│  ├─ workspace
│  ├─ channel
│  ├─ message
│  └─ file
├─ components
├─ shared
├─ hooks
└─ stores
```

원칙:

- `pages`는 라우트 진입 화면과 화면 조합만 담당한다.
- `features`는 도메인별 UI, hooks, query wrapper를 가진다.
- `components`는 도메인 비의존 공통 UI만 둔다.
- `shared`는 공통 API client, websocket client, constants, utils를 둔다.
- `stores`는 인증 보조 상태, 모달, 토스트 같은 전역 UI 상태만 둔다.

## 7. Layer Responsibility
