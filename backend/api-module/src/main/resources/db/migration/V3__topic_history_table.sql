-- V3__topic_history_table.sql
-- Adds topic history tracking for personalized topic suggestions

CREATE TABLE topic_history (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    topic VARCHAR(255) NOT NULL,
    activity_type VARCHAR(50) NOT NULL,
    use_count INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0,
    
    CONSTRAINT chk_activity_type CHECK (activity_type IN ('VOCABULARY', 'EXERCISE', 'LESSON', 'SCENARIO', 'AUDIO'))
);

-- Index for finding topics by user and activity type
CREATE INDEX idx_topic_history_user_activity ON topic_history(user_id, activity_type);

-- Index for ordering by recency
CREATE INDEX idx_topic_history_created ON topic_history(user_id, created_at DESC);

-- Unique constraint to avoid duplicate entries
CREATE UNIQUE INDEX idx_topic_history_unique ON topic_history(user_id, LOWER(topic), activity_type);

COMMENT ON TABLE topic_history IS 'Tracks topics used in learning activities to enable personalized suggestions';
COMMENT ON COLUMN topic_history.activity_type IS 'Category: VOCABULARY, EXERCISE, LESSON, SCENARIO, or AUDIO';
COMMENT ON COLUMN topic_history.use_count IS 'Number of times this topic has been used for this activity type';