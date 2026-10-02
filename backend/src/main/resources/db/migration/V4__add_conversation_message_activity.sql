ALTER TABLE conversations
    ADD COLUMN last_activity_at DATETIME(6) NULL AFTER updated_at,
    ADD COLUMN last_message_id BIGINT NULL AFTER last_activity_at;

UPDATE conversations SET last_activity_at = updated_at WHERE last_activity_at IS NULL;

ALTER TABLE conversations
    MODIFY last_activity_at DATETIME(6) NOT NULL,
    ADD CONSTRAINT fk_conversations_last_message FOREIGN KEY (last_message_id) REFERENCES messages(id) ON DELETE SET NULL;

DROP INDEX idx_conversations_activity ON conversations;
CREATE INDEX idx_conversations_activity ON conversations(last_activity_at DESC, id DESC);
CREATE INDEX idx_conversations_last_message ON conversations(last_message_id);
