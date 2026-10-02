CREATE TABLE conversations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    is_group BOOLEAN NOT NULL DEFAULT FALSE,
    name VARCHAR(100) NULL,
    direct_conversation_key VARCHAR(50) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_conversations PRIMARY KEY (id),
    CONSTRAINT uk_conversations_direct_key UNIQUE (direct_conversation_key)
);

CREATE TABLE conversation_members (
    id BIGINT NOT NULL AUTO_INCREMENT,
    conversation_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    joined_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_conversation_members PRIMARY KEY (id),
    CONSTRAINT uk_conversation_member UNIQUE (conversation_id, user_id),
    CONSTRAINT fk_conversation_members_conversation FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE,
    CONSTRAINT fk_conversation_members_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE messages (
    id BIGINT NOT NULL AUTO_INCREMENT,
    conversation_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_messages PRIMARY KEY (id),
    CONSTRAINT fk_messages_conversation FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE,
    CONSTRAINT fk_messages_sender FOREIGN KEY (sender_id) REFERENCES users(id)
);

CREATE INDEX idx_conversation_members_user_conversation ON conversation_members(user_id, conversation_id);
CREATE INDEX idx_conversations_activity ON conversations(updated_at DESC, id DESC);
CREATE INDEX idx_messages_conversation_created ON messages(conversation_id, created_at DESC);
