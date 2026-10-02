ALTER TABLE conversation_members
    ADD COLUMN last_delivered_message_id BIGINT NULL AFTER joined_at,
    ADD COLUMN last_read_message_id BIGINT NULL AFTER last_delivered_message_id;

CREATE INDEX idx_conversation_members_receipts
    ON conversation_members(user_id, conversation_id, last_read_message_id);
