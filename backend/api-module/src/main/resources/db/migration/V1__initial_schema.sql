-- Language Learning Platform - Initial Schema
-- =============================================

-- Users table
CREATE TABLE users (
    id UUID PRIMARY KEY,
    display_name VARCHAR(255) NOT NULL,
    native_language VARCHAR(2) NOT NULL,
    target_language VARCHAR(2) NOT NULL,
    skill_level VARCHAR(10) NOT NULL DEFAULT 'A1',
    assessment_completed BOOLEAN NOT NULL DEFAULT FALSE,
    timezone VARCHAR(50) NOT NULL DEFAULT 'UTC',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    version BIGINT DEFAULT 0,
    CONSTRAINT chk_skill_level CHECK (skill_level IN ('A1', 'A2', 'B1', 'B2', 'C1', 'C2'))
);

CREATE INDEX idx_users_display_name ON users(display_name);

-- Learning Goals table
CREATE TABLE learning_goals (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    goal_type VARCHAR(20) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    target_value INTEGER NOT NULL,
    current_value INTEGER NOT NULL DEFAULT 0,
    unit VARCHAR(50) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    completed_at TIMESTAMP WITH TIME ZONE,
    target_skill_level VARCHAR(10),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    version BIGINT DEFAULT 0,
    CONSTRAINT chk_goal_type CHECK (goal_type IN ('DAILY', 'WEEKLY', 'MONTHLY', 'YEARLY')),
    CONSTRAINT chk_target_skill_level CHECK (target_skill_level IS NULL OR target_skill_level IN ('A1', 'A2', 'B1', 'B2', 'C1', 'C2'))
);

CREATE INDEX idx_learning_goals_user_id ON learning_goals(user_id);
CREATE INDEX idx_learning_goals_dates ON learning_goals(start_date, end_date);
CREATE INDEX idx_learning_goals_type ON learning_goals(user_id, goal_type);

-- Exercise Results table
CREATE TABLE exercise_results (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    exercise_type VARCHAR(30) NOT NULL,
    exercise_reference VARCHAR(255),
    score INTEGER NOT NULL,
    max_score INTEGER NOT NULL DEFAULT 100,
    time_spent_seconds BIGINT NOT NULL DEFAULT 0,
    correct_answers INTEGER NOT NULL DEFAULT 0,
    total_questions INTEGER NOT NULL DEFAULT 0,
    user_response TEXT,
    correct_response TEXT,
    feedback TEXT,
    skill_level_at_time VARCHAR(10) NOT NULL,
    passed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    version BIGINT DEFAULT 0,
    CONSTRAINT chk_exercise_type CHECK (exercise_type IN (
        'TEXT_COMPLETION', 'DRAG_DROP', 'MULTIPLE_CHOICE', 'TRANSLATION',
        'LISTENING', 'SPEAKING', 'FLASHCARD', 'ROLEPLAY', 'DICTATION'
    )),
    CONSTRAINT chk_skill_level_at_time CHECK (skill_level_at_time IN ('A1', 'A2', 'B1', 'B2', 'C1', 'C2')),
    CONSTRAINT chk_score_range CHECK (score >= 0 AND score <= max_score)
);

CREATE INDEX idx_exercise_results_user_id ON exercise_results(user_id);
CREATE INDEX idx_exercise_results_created_at ON exercise_results(user_id, created_at DESC);
CREATE INDEX idx_exercise_results_type ON exercise_results(user_id, exercise_type);
CREATE INDEX idx_exercise_results_reference ON exercise_results(user_id, exercise_reference);

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Triggers for updated_at
CREATE TRIGGER update_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_learning_goals_updated_at
    BEFORE UPDATE ON learning_goals
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_exercise_results_updated_at
    BEFORE UPDATE ON exercise_results
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();