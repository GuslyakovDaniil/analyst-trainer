package com.example.dao;

import com.example.config.DatabaseConfig;
import com.example.model.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AppDao {

    public List<Topic> getAllTopics() throws SQLException {
        List<Topic> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection(); Statement st = conn.createStatement(); ResultSet rs = st.executeQuery("SELECT * FROM topics")) {
            while (rs.next()) list.add(new Topic(rs.getInt("id"), rs.getString("title")));
        }
        return list;
    }

    public List<Test> getTestsByTopic(int topicId) throws SQLException {
        List<Test> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection(); PreparedStatement ps = conn.prepareStatement("SELECT * FROM tests WHERE topic_id = ?")) {
            ps.setInt(1, topicId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(new Test(rs.getInt("id"), rs.getInt("topic_id"), rs.getString("title"), rs.getInt("time_limit"), rs.getInt("max_attempts")));
        }
        return list;
    }

    public List<Map<String, Object>> getAllTestsAdmin() throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT t.*, tp.title as topic_title FROM tests t JOIN topics tp ON t.topic_id = tp.id ORDER BY t.id DESC";
        try (Connection conn = DatabaseConfig.getConnection(); Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(Map.of("id", rs.getInt("id"), "title", rs.getString("title"), "topic", rs.getString("topic_title"), "time", rs.getInt("time_limit")));
        }
        return list;
    }

    public Test getTestById(int id) throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection(); PreparedStatement ps = conn.prepareStatement("SELECT * FROM tests WHERE id = ?")) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return new Test(rs.getInt("id"), rs.getInt("topic_id"), rs.getString("title"), rs.getInt("time_limit"), rs.getInt("max_attempts"));
        }
        return null;
    }

    public List<Question> getQuestionsByTest(int testId) throws SQLException {
        List<Question> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection(); PreparedStatement ps = conn.prepareStatement("SELECT * FROM questions WHERE test_id = ? ORDER BY id ASC")) {
            ps.setInt(1, testId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(new Question(rs.getInt("id"), rs.getInt("test_id"), rs.getString("type"), rs.getString("text"), rs.getString("options"), rs.getString("correct_answer"), rs.getInt("points")));
        }
        return list;
    }

    public Question getQuestionById(int id) throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection(); PreparedStatement ps = conn.prepareStatement("SELECT * FROM questions WHERE id = ?")) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return new Question(rs.getInt("id"), rs.getInt("test_id"), rs.getString("type"), rs.getString("text"), rs.getString("options"), rs.getString("correct_answer"), rs.getInt("points"));
        }
        return null;
    }

    public int createSession(int userId, int testId) throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection(); PreparedStatement ps = conn.prepareStatement("INSERT INTO test_sessions (user_id, test_id, status) VALUES (?, ?, 'IN_PROGRESS') RETURNING id")) {
            ps.setInt(1, userId); ps.setInt(2, testId);
            ResultSet rs = ps.executeQuery(); rs.next();
            return rs.getInt(1);
        }
    }

    public void saveAnswer(int sessionId, int qId, String ans) throws SQLException {
        Question q = getQuestionById(qId);
        int initialPoints = (q != null && q.type().equals("OPEN")) ? -1 : 0;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement("INSERT INTO test_answers (session_id, question_id, user_answer, earned_points) VALUES (?, ?, ?, ?)")) {
            ps.setInt(1, sessionId); ps.setInt(2, qId); ps.setString(3, ans); ps.setInt(4, initialPoints);
            ps.executeUpdate();
        }
    }

    public List<TestAnswer> getAnswersBySession(int sessionId) throws SQLException {
        List<TestAnswer> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection(); PreparedStatement ps = conn.prepareStatement("SELECT * FROM test_answers WHERE session_id = ?")) {
            ps.setInt(1, sessionId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(new TestAnswer(rs.getInt("id"), rs.getInt("session_id"), rs.getInt("question_id"), rs.getString("user_answer"), rs.getBoolean("is_correct"), rs.getInt("earned_points")));
        }
        return list;
    }

    public void updateAnswerGrading(int answerId, boolean correct, int points) throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection(); PreparedStatement ps = conn.prepareStatement("UPDATE test_answers SET is_correct = ?, earned_points = ? WHERE id = ?")) {
            ps.setBoolean(1, correct); ps.setInt(2, points); ps.setInt(3, answerId);
            ps.executeUpdate();
        }
    }

    public void closeSession(int sessionId, int score, String status) throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection(); PreparedStatement ps = conn.prepareStatement("UPDATE test_sessions SET score = ?, status = ?, submitted_at = CURRENT_TIMESTAMP WHERE id = ?")) {
            ps.setInt(1, score); ps.setString(2, status); ps.setInt(3, sessionId);
            ps.executeUpdate();
        }
    }

    public List<TestSession> getUserSessions(int userId) throws SQLException {
        List<TestSession> list = new ArrayList<>();
        String sql = "SELECT ts.*, t.title as t_name FROM test_sessions ts JOIN tests t ON ts.test_id = t.id WHERE ts.user_id = ? ORDER BY ts.id DESC";
        try (Connection conn = DatabaseConfig.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String date = rs.getTimestamp("submitted_at") != null ? rs.getTimestamp("submitted_at").toString() : "В процессе";
                list.add(new TestSession(rs.getInt("id"), rs.getInt("user_id"), rs.getInt("test_id"), rs.getString("status"), rs.getInt("score"), date, rs.getString("t_name")));
            }
        }
        return list;
    }

    public List<Map<String, Object>> getPendingSubmissions() throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT ta.id as answer_id, u.username, t.title, q.text, ta.user_answer, q.correct_answer, q.points FROM test_answers ta JOIN test_sessions ts ON ta.session_id = ts.id JOIN users u ON ts.user_id = u.id JOIN tests t ON ts.test_id = t.id JOIN questions q ON ta.question_id = q.id WHERE ts.status = 'PENDING' AND ta.earned_points = -1";
        try (Connection conn = DatabaseConfig.getConnection(); Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(Map.of("answerId", rs.getInt("answer_id"), "username", rs.getString("username"), "testTitle", rs.getString("title"), "questionText", rs.getString("text"), "userAnswer", rs.getString("user_answer"), "expectedAnswer", rs.getString("correct_answer"), "maxPoints", rs.getInt("points")));
        }
        return list;
    }

    public List<Map<String, Object>> getDetailedAnswers(int sessionId) throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT ta.*, q.text, q.correct_answer FROM test_answers ta JOIN questions q ON ta.question_id = q.id WHERE ta.session_id = ?";
        try (Connection conn = DatabaseConfig.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sessionId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(Map.of("question", rs.getString("text"), "userAnswer", rs.getString("user_answer") != null ? rs.getString("user_answer") : "", "expected", rs.getString("correct_answer") != null ? rs.getString("correct_answer") : "", "isCorrect", rs.getBoolean("is_correct"), "points", rs.getInt("earned_points")));
        }
        return list;
    }

    public void updateUserTotalScore(int sessionId) throws SQLException {
        String sql = "UPDATE users SET total_score = (SELECT COALESCE(SUM(ms), 0) FROM (SELECT MAX(score) as ms FROM test_sessions WHERE user_id = (SELECT user_id FROM test_sessions WHERE id = ?) AND status = 'COMPLETED' GROUP BY test_id) as t) WHERE id = (SELECT user_id FROM test_sessions WHERE id = ?)";
        try (Connection conn = DatabaseConfig.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sessionId); ps.setInt(2, sessionId); ps.executeUpdate();
        }
    }

    public void createTest(int topicId, String title, int time, int attempts) throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection(); PreparedStatement ps = conn.prepareStatement("INSERT INTO tests (topic_id, title, time_limit, max_attempts) VALUES (?, ?, ?, ?)")) {
            ps.setInt(1, topicId); ps.setString(2, title); ps.setInt(3, time); ps.setInt(4, attempts); ps.executeUpdate();
        }
    }

    public void deleteTest(int id) throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection(); PreparedStatement ps = conn.prepareStatement("DELETE FROM tests WHERE id = ?")) {
            ps.setInt(1, id); ps.executeUpdate();
        }
    }

    public void addQuestion(int testId, String type, String text, String opt, String ans, int pts) throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection(); PreparedStatement ps = conn.prepareStatement("INSERT INTO questions (test_id, type, text, options, correct_answer, points) VALUES (?, ?, ?, ?, ?, ?)")) {
            ps.setInt(1, testId); ps.setString(2, type); ps.setString(3, text); ps.setString(4, opt); ps.setString(5, ans); ps.setInt(6, pts); ps.executeUpdate();
        }
    }

    public void updateQuestion(int qId, String type, String text, String opt, String ans, int pts) throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection(); PreparedStatement ps = conn.prepareStatement("UPDATE questions SET type=?, text=?, options=?, correct_answer=?, points=? WHERE id=?")) {
            ps.setString(1, type); ps.setString(2, text); ps.setString(3, opt); ps.setString(4, ans); ps.setInt(5, pts); ps.setInt(6, qId); ps.executeUpdate();
        }
    }

    public void deleteQuestion(int qId) throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection(); PreparedStatement ps = conn.prepareStatement("DELETE FROM questions WHERE id = ?")) {
            ps.setInt(1, qId); ps.executeUpdate();
        }
    }

    public int countAttempts(int userId, int testId) throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection(); PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM test_sessions WHERE user_id = ? AND test_id = ?")) {
            ps.setInt(1, userId); ps.setInt(2, testId);
            ResultSet rs = ps.executeQuery(); return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public Integer getSessionIdByAnswer(int answerId) throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection(); PreparedStatement ps = conn.prepareStatement("SELECT session_id FROM test_answers WHERE id = ?")) {
            ps.setInt(1, answerId);
            ResultSet rs = ps.executeQuery(); return rs.next() ? rs.getInt(1) : null;
        }
    }

    public boolean isAllQuestionsGraded(int sessionId) throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection(); PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM test_answers WHERE session_id = ? AND earned_points = -1")) {
            ps.setInt(1, sessionId);
            ResultSet rs = ps.executeQuery(); return rs.next() && rs.getInt(1) == 0;
        }
    }

    public int calculateSessionScore(int sessionId) throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection(); PreparedStatement ps = conn.prepareStatement("SELECT SUM(earned_points) FROM test_answers WHERE session_id = ? AND earned_points > 0")) {
            ps.setInt(1, sessionId);
            ResultSet rs = ps.executeQuery(); return rs.next() ? rs.getInt(1) : 0;
        }
    }
}