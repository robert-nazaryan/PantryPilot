CREATE TABLE chat_session (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_chat_session_user_id ON chat_session (user_id);

CREATE TABLE chat_message (
    id               BIGSERIAL PRIMARY KEY,
    chat_session_id  BIGINT      NOT NULL REFERENCES chat_session(id) ON DELETE CASCADE,
    role             VARCHAR(20) NOT NULL,
    content          TEXT        NOT NULL,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chat_message_role_chk
        CHECK (role IN ('USER', 'ASSISTANT'))
);

CREATE INDEX idx_chat_message_session_id ON chat_message (chat_session_id, created_at);
