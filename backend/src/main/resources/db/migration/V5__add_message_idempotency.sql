ALTER TABLE messages
    ADD COLUMN client_message_id VARCHAR(64) NULL AFTER content;

UPDATE messages SET client_message_id = CONCAT('legacy-', id) WHERE client_message_id IS NULL;

ALTER TABLE messages
    MODIFY client_message_id VARCHAR(64) NOT NULL,
    ADD CONSTRAINT uk_messages_sender_client_id UNIQUE (sender_id, client_message_id);
