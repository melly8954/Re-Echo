DROP INDEX idx_workspace_memberships_user_status_last_visited;

CREATE INDEX idx_workspace_memberships_user_status_joined
    ON workspace_memberships (user_id, status, joined_at ASC, id ASC);
