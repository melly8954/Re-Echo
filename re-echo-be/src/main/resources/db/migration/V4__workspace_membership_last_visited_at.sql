ALTER TABLE workspace_memberships
    ADD COLUMN last_visited_at timestamptz;

UPDATE workspace_memberships
SET last_visited_at = joined_at
WHERE last_visited_at IS NULL;

ALTER TABLE workspace_memberships
    ALTER COLUMN last_visited_at SET NOT NULL,
    ALTER COLUMN last_visited_at SET DEFAULT now();

DROP INDEX idx_workspace_memberships_user_status;

CREATE INDEX idx_workspace_memberships_user_status_last_visited
    ON workspace_memberships (user_id, status, last_visited_at DESC);
