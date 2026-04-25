CREATE TABLE IF NOT EXISTS users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(32) NOT NULL UNIQUE,
  password_hash VARCHAR(128) NOT NULL,
  display_name VARCHAR(32) NOT NULL,
  role VARCHAR(24) NOT NULL DEFAULT 'STUDENT',
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
  source_math_question_id BIGINT NULL,
  chapter_id BIGINT NOT NULL,
  title VARCHAR(180) NOT NULL,
  content TEXT NOT NULL,
  type VARCHAR(24) NOT NULL DEFAULT 'FILL',
  options_json TEXT NULL,
  answer_json TEXT NULL,
  explanation TEXT NULL,
  difficulty VARCHAR(24) NOT NULL DEFAULT 'MEDIUM',
  source_year INT NULL,
  source_paper VARCHAR(80) NULL,
  source_question_no VARCHAR(24) NULL,
  source_label VARCHAR(120) NULL,
  source_doc_path VARCHAR(1024) NULL,
  source_snapshot_path VARCHAR(1024) NULL,
  exam_section VARCHAR(40) NULL,
  import_batch VARCHAR(80) NULL,
  import_status VARCHAR(40) NOT NULL DEFAULT 'READY',
  classification_confidence DECIMAL(5,2) NULL,
  explanation_source VARCHAR(40) NOT NULL DEFAULT 'NONE',
  explanation_review_status VARCHAR(40) NOT NULL DEFAULT 'PENDING_REVIEW',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_question_chapter FOREIGN KEY (chapter_id) REFERENCES chapters(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS math_questions (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  image_url VARCHAR(1024) NULL,
  raw_text_latex LONGTEXT NOT NULL,
  answer_latex TEXT NULL,
  teacher_explanation LONGTEXT NULL,
  book_name VARCHAR(120) NOT NULL,
  chapter_name VARCHAR(160) NOT NULL,
  section_name VARCHAR(180) NOT NULL,
  source_year INT NULL,
  source_paper VARCHAR(80) NULL,
  question_no INT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS mistake_records (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  question_id BIGINT NULL,
  chapter_id BIGINT NULL,
  difficulty VARCHAR(24) NULL,
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
  question_id BIGINT NULL,
  chapter_id BIGINT NULL,
  difficulty VARCHAR(24) NULL,
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
  repetition INT NOT NULL DEFAULT 0,
  interval_days INT NOT NULL DEFAULT 1,
  ease_factor DECIMAL(4,2) NOT NULL DEFAULT 2.50,
  suspended TINYINT(1) NOT NULL DEFAULT 0,
  last_grade VARCHAR(24) NULL,
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

CREATE TABLE IF NOT EXISTS study_sessions (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  chapter_id BIGINT NULL,
  duration_seconds INT NOT NULL DEFAULT 1800,
  started_at DATETIME NOT NULL,
  submitted_at DATETIME NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  CONSTRAINT fk_study_session_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_study_session_chapter FOREIGN KEY (chapter_id) REFERENCES chapters(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS study_session_items (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  session_id BIGINT NOT NULL,
  question_id BIGINT NOT NULL,
  sort_order INT NOT NULL,
  user_answer_json TEXT NULL,
  is_correct TINYINT(1) NULL,
  answered_at DATETIME NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  CONSTRAINT fk_study_item_session FOREIGN KEY (session_id) REFERENCES study_sessions(id) ON DELETE CASCADE,
  CONSTRAINT fk_study_item_question FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE
);

CREATE INDEX idx_mistake_user ON mistake_records(user_id);
CREATE INDEX idx_mistake_question ON mistake_records(question_id);
CREATE INDEX idx_review_user ON review_tasks(user_id);
CREATE INDEX idx_review_due ON review_tasks(user_id, due_date);
CREATE INDEX idx_favorite_user ON favorites(user_id);
CREATE INDEX idx_favorite_question ON favorites(question_id);
CREATE INDEX idx_question_chapter ON questions(chapter_id);
CREATE INDEX idx_question_source_math ON questions(source_math_question_id);
CREATE INDEX idx_question_source ON questions(source_year, source_paper, source_question_no);
CREATE INDEX idx_question_import_batch ON questions(import_batch);
CREATE INDEX idx_math_question_source ON math_questions(source_year, source_paper, question_no);
CREATE INDEX idx_math_question_section ON math_questions(book_name, chapter_name, section_name);
CREATE INDEX idx_qa_session_user ON qa_sessions(user_id);
CREATE INDEX idx_study_session_user ON study_sessions(user_id);
CREATE INDEX idx_study_item_session ON study_session_items(session_id);
