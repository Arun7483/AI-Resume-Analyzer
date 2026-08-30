CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,

    email VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,

    role VARCHAR(20) NOT NULL DEFAULT 'ROLE_USER',

    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    verification_token VARCHAR(64),
    verification_expires_at TIMESTAMP(6),

    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_users_email UNIQUE (email),

    CONSTRAINT chk_users_role
        CHECK (role IN ('ROLE_USER', 'ROLE_ADMIN'))
);

CREATE INDEX idx_users_email
    ON users(email);


CREATE TABLE resumes (
    id BIGSERIAL PRIMARY KEY,

    user_id BIGINT NOT NULL,

    file_name VARCHAR(255) NOT NULL,
    storage_key VARCHAR(512) NOT NULL,
    raw_text TEXT NOT NULL,

    uploaded_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_resumes_storage_key
        UNIQUE (storage_key),

    CONSTRAINT fk_resumes_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_resumes_user
    ON resumes(user_id);

CREATE INDEX idx_resumes_uploaded
    ON resumes(uploaded_at);


CREATE TABLE analysis_results (
    id BIGSERIAL PRIMARY KEY,

    resume_id BIGINT NOT NULL,

    overall_score INT NOT NULL,
    ats_match_percentage INT NOT NULL,

    strengths_json JSONB NOT NULL,
    weaknesses_json JSONB NOT NULL,
    missing_keywords_json JSONB NOT NULL,

    analyzed_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_analysis_resume
        FOREIGN KEY (resume_id)
        REFERENCES resumes(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_overall_score
        CHECK (overall_score BETWEEN 0 AND 100),

    CONSTRAINT chk_ats_score
        CHECK (ats_match_percentage BETWEEN 0 AND 100)
);

CREATE INDEX idx_analysis_resume
    ON analysis_results(resume_id);

CREATE INDEX idx_analysis_date
    ON analysis_results(analyzed_at);


CREATE TABLE chat_messages (
    id BIGSERIAL PRIMARY KEY,

    user_id BIGINT NOT NULL,
    resume_id BIGINT,

    sender VARCHAR(10) NOT NULL,
    content TEXT NOT NULL,

    timestamp TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_chat_sender
        CHECK (sender IN ('USER', 'BOT')),

    CONSTRAINT fk_chat_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_chat_resume
        FOREIGN KEY (resume_id)
        REFERENCES resumes(id)
        ON DELETE SET NULL
);

CREATE INDEX idx_chat_user_time
    ON chat_messages(user_id, timestamp);

CREATE INDEX idx_chat_resume
    ON chat_messages(resume_id);