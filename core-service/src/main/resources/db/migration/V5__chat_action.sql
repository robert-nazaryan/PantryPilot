CREATE TABLE chat_action (
    id               BIGSERIAL PRIMARY KEY,
    chat_session_id  BIGINT      NOT NULL REFERENCES chat_session(id) ON DELETE CASCADE,
    type             VARCHAR(50) NOT NULL,
    status           VARCHAR(20) NOT NULL,
    payload_json     TEXT        NOT NULL,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    confirmed_at     TIMESTAMPTZ,
    CONSTRAINT chat_action_type_chk
        CHECK (type IN ('CREATE_PANTRY_ITEM')),
    CONSTRAINT chat_action_status_chk
        CHECK (status IN ('PENDING', 'CONFIRMED'))
);

CREATE INDEX idx_chat_action_session_id ON chat_action (chat_session_id, created_at);
