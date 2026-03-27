-- Chat Service Tables
-- ====================

-- Chat sessions table
CREATE TABLE chat_sessions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    message_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    version BIGINT DEFAULT 0
);

CREATE INDEX idx_chat_sessions_user_id ON chat_sessions(user_id);
CREATE INDEX idx_chat_sessions_active ON chat_sessions(user_id, active);
CREATE INDEX idx_chat_sessions_created_at ON chat_sessions(user_id, created_at DESC);

-- Chat messages table
CREATE TABLE chat_messages (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL REFERENCES chat_sessions(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL,
    content TEXT,
    embedded_activity_type VARCHAR(30),
    embedded_activity_content TEXT,
    activity_completed BOOLEAN NOT NULL DEFAULT FALSE,
    activity_summary TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    version BIGINT DEFAULT 0,
    CONSTRAINT chk_message_role CHECK (role IN ('USER', 'ASSISTANT', 'SYSTEM')),
    CONSTRAINT chk_embedded_activity_type CHECK (
        embedded_activity_type IS NULL OR 
        embedded_activity_type IN (
            'VOCABULARY', 'FLASHCARDS', 'VISUAL_CARDS', 
            'TEXT_COMPLETION', 'DRAG_DROP', 'TRANSLATION',
            'LISTENING', 'SPEAKING', 'LESSON', 'SCENARIO',
            'LEARNING_PLAN', 'SUMMARY'
        )
    )
);

CREATE INDEX idx_chat_messages_session_id ON chat_messages(session_id);
CREATE INDEX idx_chat_messages_created_at ON chat_messages(session_id, created_at ASC);
CREATE INDEX idx_chat_messages_pending_activity ON chat_messages(session_id, activity_completed) 
    WHERE embedded_activity_type IS NOT NULL AND activity_completed = FALSE;

-- Triggers for updated_at
CREATE TRIGGER update_chat_sessions_updated_at
    BEFORE UPDATE ON chat_sessions
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_chat_messages_updated_at
    BEFORE UPDATE ON chat_messages
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();