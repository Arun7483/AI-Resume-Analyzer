CREATE TABLE users (
 id BIGINT NOT NULL AUTO_INCREMENT, email VARCHAR(255) NOT NULL, password VARCHAR(255) NOT NULL,
 full_name VARCHAR(100) NOT NULL, role ENUM('ROLE_USER','ROLE_ADMIN') NOT NULL DEFAULT 'ROLE_USER',
 created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6), updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
 PRIMARY KEY (id), UNIQUE KEY uk_users_email (email), KEY idx_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE resumes (
 id BIGINT NOT NULL AUTO_INCREMENT, user_id BIGINT NOT NULL, file_name VARCHAR(255) NOT NULL, storage_key VARCHAR(512) NOT NULL,
 raw_text LONGTEXT NOT NULL, uploaded_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6), PRIMARY KEY(id), UNIQUE KEY uk_resumes_storage_key(storage_key),
 KEY idx_resumes_user(user_id), KEY idx_resumes_uploaded(uploaded_at), CONSTRAINT fk_resumes_user FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE analysis_results (
 id BIGINT NOT NULL AUTO_INCREMENT, resume_id BIGINT NOT NULL, overall_score INT NOT NULL, ats_match_percentage INT NOT NULL,
 strengths_json JSON NOT NULL, weaknesses_json JSON NOT NULL, missing_keywords_json JSON NOT NULL, analyzed_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 PRIMARY KEY(id), KEY idx_analysis_resume(resume_id), KEY idx_analysis_date(analyzed_at), CONSTRAINT fk_analysis_resume FOREIGN KEY(resume_id) REFERENCES resumes(id) ON DELETE CASCADE,
 CONSTRAINT chk_overall_score CHECK(overall_score BETWEEN 0 AND 100), CONSTRAINT chk_ats_score CHECK(ats_match_percentage BETWEEN 0 AND 100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE chat_messages (
 id BIGINT NOT NULL AUTO_INCREMENT, user_id BIGINT NOT NULL, resume_id BIGINT NULL, sender ENUM('USER','BOT') NOT NULL, content TEXT NOT NULL,
 timestamp TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6), PRIMARY KEY(id), KEY idx_chat_user_time(user_id,timestamp), KEY idx_chat_resume(resume_id),
 CONSTRAINT fk_chat_user FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE, CONSTRAINT fk_chat_resume FOREIGN KEY(resume_id) REFERENCES resumes(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
