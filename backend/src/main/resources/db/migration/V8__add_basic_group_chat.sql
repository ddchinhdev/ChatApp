ALTER TABLE conversations
    ADD COLUMN avatar_url VARCHAR(500) NULL AFTER name;

ALTER TABLE conversation_members
    ADD COLUMN group_role VARCHAR(20) NOT NULL DEFAULT 'MEMBER' AFTER joined_at;

CREATE INDEX idx_conversation_members_conversation_role
    ON conversation_members(conversation_id, group_role);

ALTER TABLE messages
    ADD COLUMN is_system BOOLEAN NOT NULL DEFAULT FALSE AFTER client_message_id;
