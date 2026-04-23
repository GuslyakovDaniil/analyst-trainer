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

-- Тестовые данные (Выполнение требования хакатона)
INSERT INTO topics (title) VALUES ('Системный анализ: Практика');

INSERT INTO tests (topic_id, title, time_limit, max_attempts) VALUES
(1, 'Итоговый ассессмент (Тесты, Ошибки, Артефакты)', 45, 5);

INSERT INTO questions (test_id, type, text, options, correct_answer, points) VALUES
-- 1. ТЕСТОВЫЕ ЗАДАНИЯ (3 шт) - один и несколько вариантов
(1, 'TEST', 'Какая нотация используется для моделирования бизнес-процессов?', 'UML;BPMN;C4;ERD', 'BPMN', 10),
(1, 'TEST', 'Выберите все артефакты, описывающие требования к системе (несколько ответов):', 'User Story;Use Case;Смета проекта;Матрица RACI', 'User Story;Use Case', 10),
(1, 'TEST', 'Что из перечисленного относится к нефункциональным требованиям (NFR)? (несколько ответов)', 'Кнопка "Скачать";Отказоустойчивость 99.9%;Время отклика менее 2с;Интеграция по REST API', 'Отказоустойчивость 99.9%;Время отклика менее 2с', 10),

-- 2. ЗАДАНИЯ НА ПОИСК ОШИБОК (3 шт) - тип BUG (Интерактивные выпадающие списки)
(1, 'BUG', 'Анализ требований (Текст): В техническом задании написано: "Приложение [dropdown:должно;желательно чтобы;может] выдерживать пиковую нагрузку в 5000 rps. Это относится к [dropdown:нефункциональным;функциональным;бизнес] требованиям. Для проверки этого критерия нужно провести [dropdown:нагрузочное;регрессионное;модульное] тестирование."', NULL, 'должно;нефункциональным;нагрузочное', 20),
(1, 'BUG', 'Анализ SQL-запроса: Джуниор-аналитик написал запрос для подсчета количества активных пользователей по городам. Исправьте ошибки в синтаксисе:
SELECT city, [dropdown:COUNT(*);SUM(id);MAX(users)]
FROM users
[dropdown:WHERE;HAVING;FILTER] status = ''active''
[dropdown:GROUP BY;ORDER BY;PARTITION BY] city', NULL, 'COUNT(*);WHERE;GROUP BY', 20),
(1, 'BUG', 'Проектирование API: Разработчик спроектировал REST API метод для создания нового пользователя. Выберите правильные стандарты:
HTTP-метод: [dropdown:POST;GET;PUT;DELETE]
URL: /api/v1/[dropdown:users;createUser;getUsers]
Код успешного ответа: [dropdown:201 Created;200 OK;404 Not Found]', NULL, 'POST;users;201 Created', 20),

-- 3. ОТКРЫТЫЕ ЗАДАНИЯ (4 шт) - самостоятельная проработка
(1, 'OPEN', 'Разработайте структуру таблицы для сущности "Заказ" (Orders) с указанием типов данных.', NULL, 'Ожидается перечисление id, user_id, status, total_amount, created_at и т.д.', 20),
(1, 'OPEN', 'Опишите основные граничные значения для проверки поля "Возраст пользователя" (допустимый диапазон 18-65 лет).', NULL, '17, 18, 19, 64, 65, 66', 20),
(1, 'OPEN', 'Спроектируйте JSON-структуру ответа сервера при успешной авторизации пользователя.', NULL, '{ "token": "...", "user": { "id": 1, "name": "..." } }', 20),
(1, 'OPEN', 'Опишите альтернативный сценарий (Alternative Flow) для Use Case "Восстановление пароля" (если email не найден в системе).', NULL, 'Система сообщает, что инструкция отправлена (для безопасности), но письмо не отправляется / логгируется попытка.', 20);