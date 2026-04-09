CREATE TABLE IF NOT EXISTS users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(32) NOT NULL UNIQUE,
  password_hash VARCHAR(128) NOT NULL,
  display_name VARCHAR(32) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS chapters (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  parent_id BIGINT NULL,
  title VARCHAR(120) NOT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_chapter_parent FOREIGN KEY (parent_id) REFERENCES chapters(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS questions (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  chapter_id BIGINT NOT NULL,
  title VARCHAR(180) NOT NULL,
  content TEXT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_question_chapter FOREIGN KEY (chapter_id) REFERENCES chapters(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS mistake_records (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  chapter_id BIGINT NULL,
  question_title VARCHAR(180) NOT NULL,
  question_content TEXT NOT NULL,
  image_url VARCHAR(1024) NULL,
  status VARCHAR(24) NOT NULL DEFAULT 'NEW',
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  CONSTRAINT fk_mistake_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_mistake_chapter FOREIGN KEY (chapter_id) REFERENCES chapters(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS mistake_analysis (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  mistake_id BIGINT NOT NULL UNIQUE,
  knowledge_points_json TEXT NOT NULL,
  error_type VARCHAR(120) NOT NULL,
  solving_steps_json TEXT NOT NULL,
  variants_json TEXT NOT NULL,
  follow_up_json TEXT NOT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  CONSTRAINT fk_analysis_mistake FOREIGN KEY (mistake_id) REFERENCES mistake_records(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS favorites (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  title VARCHAR(180) NOT NULL,
  content TEXT NOT NULL,
  created_at DATETIME NOT NULL,
  CONSTRAINT fk_favorite_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS review_tasks (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  mistake_id BIGINT NOT NULL,
  due_date DATETIME NOT NULL,
  completed TINYINT(1) NOT NULL DEFAULT 0,
  completed_at DATETIME NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  CONSTRAINT fk_review_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_review_mistake FOREIGN KEY (mistake_id) REFERENCES mistake_records(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS qa_sessions (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  title VARCHAR(80) NOT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  CONSTRAINT fk_qa_session_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS qa_messages (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  session_id BIGINT NOT NULL,
  role VARCHAR(24) NOT NULL,
  content TEXT NOT NULL,
  created_at DATETIME NOT NULL,
  CONSTRAINT fk_qa_message_session FOREIGN KEY (session_id) REFERENCES qa_sessions(id) ON DELETE CASCADE
);

CREATE INDEX idx_mistake_user ON mistake_records(user_id);
CREATE INDEX idx_review_user ON review_tasks(user_id);
CREATE INDEX idx_favorite_user ON favorites(user_id);
CREATE INDEX idx_question_chapter ON questions(chapter_id);
CREATE INDEX idx_qa_session_user ON qa_sessions(user_id);
