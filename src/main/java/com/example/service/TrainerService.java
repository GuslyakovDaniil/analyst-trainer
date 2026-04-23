package com.example.service;

import com.example.dao.AppDao;
import com.example.dao.UserDao;
import com.example.model.*;
import com.example.config.DatabaseConfig;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class TrainerService {
    private final AppDao appDao = new AppDao();
    private final UserDao userDao = new UserDao();

    public void processTestSubmission(int sessionId) throws SQLException {
        List<TestAnswer> answers = appDao.getAnswersBySession(sessionId);
        int autoScore = 0;
        boolean hasManualCheck = false;
        boolean hasCorrectSql = false;

        for (TestAnswer ans : answers) {
            Question q = appDao.getQuestionById(ans.questionId());
            if (q.type().equals("OPEN")) {
                hasManualCheck = true;
            } else {
                boolean correct = false;
                String expected = q.correctAnswer() != null ? q.correctAnswer().trim() : "";
                String actual = ans.userAnswer() != null ? ans.userAnswer().trim() : "";

                if (q.type().equals("TEST") && expected.contains(";")) {
                    java.util.Set<String> expSet = new java.util.HashSet<>(java.util.Arrays.asList(expected.toLowerCase().split(";")));
                    java.util.Set<String> actSet = new java.util.HashSet<>(java.util.Arrays.asList(actual.toLowerCase().split(";")));
                    correct = expSet.equals(actSet);
                }
                else {
                    correct = expected.equalsIgnoreCase(actual);
                }

                int points = correct ? q.points() : 0;
                appDao.updateAnswerGrading(ans.id(), correct, points);
                autoScore += points;
                if (correct && q.type().equals("SQL")) hasCorrectSql = true;
            }
        }

        String status = hasManualCheck ? "PENDING" : "COMPLETED";
        appDao.closeSession(sessionId, autoScore, status);

        if (status.equals("COMPLETED")) {
            appDao.updateUserTotalScore(sessionId);
            checkAchievements(sessionId, hasCorrectSql);
        }
    }

    public void adminGradeAnswer(int answerId, int points) throws SQLException {
        appDao.updateAnswerGrading(answerId, points > 0, points);
        Integer sid = appDao.getSessionIdByAnswer(answerId);

        if (sid != null && appDao.isAllQuestionsGraded(sid)) {
            int finalScore = appDao.calculateSessionScore(sid);
            appDao.closeSession(sid, finalScore, "COMPLETED");
            appDao.updateUserTotalScore(sid);
            checkAchievements(sid, false);
        }
    }

    private void checkAchievements(int sessionId, boolean hasCorrectSql) throws SQLException {
        int userId = getUserIdBySession(sessionId);
        int score = appDao.calculateSessionScore(sessionId);
        int totalSessions = appDao.getUserSessions(userId).size();

        userDao.awardAchievement(userId, "Первый шаг");
        if (score >= 40) userDao.awardAchievement(userId, "Аналитик уровня Бог");
        if (totalSessions >= 3) userDao.awardAchievement(userId, "Опытный боец");
        if (hasCorrectSql) userDao.awardAchievement(userId, "Мастер SQL");
    }

    private int getUserIdBySession(int sessionId) throws SQLException {
        try (Connection c = DatabaseConfig.getConnection(); PreparedStatement ps = c.prepareStatement("SELECT user_id FROM test_sessions WHERE id=?")) {
            ps.setInt(1, sessionId); ResultSet rs = ps.executeQuery();
            rs.next(); return rs.getInt(1);
        }
    }
}