DROP TABLE IF EXISTS user_achievements CASCADE;
DROP TABLE IF EXISTS test_answers CASCADE;
DROP TABLE IF EXISTS test_sessions CASCADE;
DROP TABLE IF EXISTS questions CASCADE;
DROP TABLE IF EXISTS tests CASCADE;
DROP TABLE IF EXISTS topics CASCADE;
DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS achievements_dict CASCADE;

CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(100) NOT NULL,
    role VARCHAR(10) DEFAULT 'USER',
    total_score INTEGER DEFAULT 0,
    is_public BOOLEAN DEFAULT TRUE,
    avatar_icon VARCHAR(10) DEFAULT '👤'
);

-- Справочник всех существующих ачивок
CREATE TABLE achievements_dict (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL,
    icon VARCHAR(10) NOT NULL,
    description TEXT NOT NULL
);

CREATE TABLE user_achievements (
    id SERIAL PRIMARY KEY,
    user_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
    achievement_id INTEGER REFERENCES achievements_dict(id) ON DELETE CASCADE,
    earned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, achievement_id)
);

CREATE TABLE topics (id SERIAL PRIMARY KEY, title VARCHAR(255) NOT NULL);
CREATE TABLE tests (id SERIAL PRIMARY KEY, topic_id INTEGER REFERENCES topics(id) ON DELETE CASCADE, title VARCHAR(255) NOT NULL, time_limit INTEGER NOT NULL, max_attempts INTEGER DEFAULT 1);
CREATE TABLE questions (id SERIAL PRIMARY KEY, test_id INTEGER REFERENCES tests(id) ON DELETE CASCADE, type VARCHAR(20) NOT NULL, text TEXT NOT NULL, options TEXT, correct_answer TEXT, points INTEGER NOT NULL);
CREATE TABLE test_sessions (id SERIAL PRIMARY KEY, user_id INTEGER REFERENCES users(id) ON DELETE CASCADE, test_id INTEGER REFERENCES tests(id) ON DELETE CASCADE, started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, submitted_at TIMESTAMP, status VARCHAR(20) DEFAULT 'IN_PROGRESS', score INTEGER DEFAULT 0);
CREATE TABLE test_answers (id SERIAL PRIMARY KEY, session_id INTEGER REFERENCES test_sessions(id) ON DELETE CASCADE, question_id INTEGER REFERENCES questions(id) ON DELETE CASCADE, user_answer TEXT, is_correct BOOLEAN DEFAULT FALSE, earned_points INTEGER DEFAULT -1);

-- Наполнение справочника ачивок
INSERT INTO achievements_dict (name, icon, description) VALUES
('Первый шаг', '🚀', 'Сдайте свой первый тест на любую оценку.'),
('Аналитик уровня Бог', '👑', 'Наберите 40 или более баллов за один тест.'),
('Опытный боец', '⚔️', 'Пройдите 3 любых теста.'),
('Мастер SQL', '💾', 'Правильно решите задачу типа SQL.');

-- Тестовые данные
INSERT INTO topics (title) VALUES ('Системный анализ'), ('SQL');
INSERT INTO tests (topic_id, title, time_limit, max_attempts) VALUES (1, 'Основы требований', 10, 3);
INSERT INTO questions (test_id, type, text, options, correct_answer, points) VALUES
(1, 'TEST', 'Что такое NFR?', 'Бюджет;Качество системы;Сроки', 'Качество системы', 10),
(1, 'OPEN', 'Опишите структуру User Story', NULL, 'Я как [роль], хочу [действие], чтобы [ценность]', 40);