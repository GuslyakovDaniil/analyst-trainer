package com.example.controller;

import io.javalin.http.Context;
import com.example.dao.AppDao;
import com.example.dao.UserDao;
import com.example.model.*;
import com.example.service.TrainerService;
import com.example.util.SecurityUtil;

import java.util.Map;
import java.util.List;

public class AppController {
    private static final AppDao appDao = new AppDao();
    private static final UserDao userDao = new UserDao();
    private static final TrainerService trainerService = new TrainerService();

    // --- ПОЛЬЗОВАТЕЛЬСКОЕ API ---
    public static void getTopics(Context ctx) throws Exception { ctx.json(appDao.getAllTopics()); }

    public static void getTests(Context ctx) throws Exception {
        String topicIdStr = ctx.queryParam("topicId");
        if (topicIdStr == null || topicIdStr.isEmpty()) { ctx.status(400); return; }
        ctx.json(appDao.getTestsByTopic(Integer.parseInt(topicIdStr)));
    }

    public static void startTest(Context ctx) throws Exception {
        int testId = Integer.parseInt(ctx.pathParam("id"));
        User user = userDao.findByUsername(ctx.attribute("username"));
        Test test = appDao.getTestById(testId);
        if (test == null) { ctx.status(404); return; }

        if (appDao.countAttempts(user.id(), testId) >= test.maxAttempts()) { ctx.status(403).result("Попытки исчерпаны"); return; }

        int sid = appDao.createSession(user.id(), testId);
        ctx.json(Map.of("sessionId", sid, "questions", appDao.getQuestionsByTest(testId)));
    }

    @SuppressWarnings("unchecked")
    public static void submitTest(Context ctx) throws Exception {
        int sid = Integer.parseInt(ctx.pathParam("id"));
        Map<String, String> answers = ctx.bodyAsClass(Map.class);
        for (Map.Entry<String, String> entry : answers.entrySet()) {
            appDao.saveAnswer(sid, Integer.parseInt(entry.getKey()), entry.getValue());
        }
        trainerService.processTestSubmission(sid);
        ctx.json(Map.of("status", "ok"));
    }

    public static void getUserSessionDetails(Context ctx) throws Exception {
        ctx.json(appDao.getDetailedAnswers(Integer.parseInt(ctx.pathParam("id"))));
    }

    // --- ПОЛЬЗОВАТЕЛЬСКОЕ API (ПРОФИЛЬ, НАСТРОЙКИ, АЧИВКИ) ---
    public static void getProfile(Context ctx) throws Exception {
        User user = userDao.findByUsername(ctx.attribute("username"));
        if (user == null) { ctx.status(401); return; }
        ctx.json(Map.of(
                "user", user,
                "sessions", appDao.getUserSessions(user.id()),
                "achievements", userDao.getAllAchievementsWithStatus(user.id()), // Отдаем ВСЕ ачивки, чтобы показать каталог
                "isPublic", userDao.isUserPublic(user.id())
        ));
    }

    public static void getLeaderboard(Context ctx) throws Exception { ctx.json(userDao.getLeaderboard()); }

    @SuppressWarnings("unchecked")
    public static void togglePrivacy(Context ctx) throws Exception {
        User user = userDao.findByUsername(ctx.attribute("username"));
        Map<String, Boolean> body = ctx.bodyAsClass(Map.class);
        userDao.togglePrivacy(user.id(), body.get("isPublic"));
        ctx.status(200);
    }

    @SuppressWarnings("unchecked")
    public static void updateProfile(Context ctx) throws Exception {
        User user = userDao.findByUsername(ctx.attribute("username"));
        Map<String, String> body = ctx.bodyAsClass(Map.class);

        if (body.containsKey("username")) {
            String newName = body.get("username").trim();
            if (!newName.isEmpty()) {
                userDao.updateUsername(user.id(), newName);
                // Обновляем токен, так как имя пользователя изменилось
                String newToken = SecurityUtil.generateToken(newName);
                ctx.json(Map.of("newToken", newToken, "username", newName));
                return;
            }
        }

        if (body.containsKey("avatar")) {
            userDao.updateAvatar(user.id(), body.get("avatar"));
        }
        ctx.status(200).result("Обновлено");
    }

    // --- АДМИНСКОЕ API (СТАТИСТИКА И ПРОВЕРКА) ---
    public static void getAdminUsers(Context ctx) throws Exception {
        if (!isAdmin(ctx)) return;
        ctx.json(userDao.findAllUsers());
    }

    public static void getAdminPending(Context ctx) throws Exception {
        if (!isAdmin(ctx)) return;
        ctx.json(appDao.getPendingSubmissions());
    }

    @SuppressWarnings("unchecked")
    public static void gradeAnswer(Context ctx) throws Exception {
        if (!isAdmin(ctx)) return;
        Map<String, Object> body = ctx.bodyAsClass(Map.class);
        trainerService.adminGradeAnswer(((Number) body.get("answerId")).intValue(), ((Number) body.get("points")).intValue());
        ctx.status(200).result("Оценка сохранена");
    }

    public static void getAdminSessions(Context ctx) throws Exception {
        if (!isAdmin(ctx)) return;
        ctx.json(appDao.getUserSessions(Integer.parseInt(ctx.pathParam("id"))));
    }

    public static void getAdminSessionDetails(Context ctx) throws Exception {
        if (!isAdmin(ctx)) return;
        ctx.json(appDao.getDetailedAnswers(Integer.parseInt(ctx.pathParam("id"))));
    }

    // --- АДМИНСКОЕ API (УПРАВЛЕНИЕ ТЕСТАМИ) ---
    public static void getAdminAllTests(Context ctx) throws Exception {
        if (!isAdmin(ctx)) return;
        ctx.json(appDao.getAllTestsAdmin());
    }

    @SuppressWarnings("unchecked")
    public static void createTest(Context ctx) throws Exception {
        if (!isAdmin(ctx)) return;
        Map<String, Object> body = ctx.bodyAsClass(Map.class);
        int topicId = ((Number) body.get("topicId")).intValue();
        String title = (String) body.get("title");
        int time = body.containsKey("time") ? ((Number) body.get("time")).intValue() : 15;
        int attempts = body.containsKey("attempts") ? ((Number) body.get("attempts")).intValue() : 3;

        appDao.createTest(topicId, title, time, attempts);
        ctx.status(201).result("Тест создан");
    }

    public static void deleteTest(Context ctx) throws Exception {
        if (!isAdmin(ctx)) return;
        appDao.deleteTest(Integer.parseInt(ctx.pathParam("id")));
        ctx.status(204);
    }

    public static void getTestQuestions(Context ctx) throws Exception {
        if (!isAdmin(ctx)) return;
        ctx.json(appDao.getQuestionsByTest(Integer.parseInt(ctx.pathParam("id"))));
    }

    @SuppressWarnings("unchecked")
    public static void addQuestion(Context ctx) throws Exception {
        if (!isAdmin(ctx)) return;
        Map<String, Object> b = ctx.bodyAsClass(Map.class);
        appDao.addQuestion(((Number)b.get("testId")).intValue(), (String)b.get("type"), (String)b.get("text"), (String)b.get("options"), (String)b.get("answer"), ((Number)b.get("points")).intValue());
        ctx.status(201).result("Вопрос добавлен");
    }

    @SuppressWarnings("unchecked")
    public static void updateQuestion(Context ctx) throws Exception {
        if (!isAdmin(ctx)) return;
        Map<String, Object> b = ctx.bodyAsClass(Map.class);
        appDao.updateQuestion(Integer.parseInt(ctx.pathParam("id")), (String)b.get("type"), (String)b.get("text"), (String)b.get("options"), (String)b.get("answer"), ((Number)b.get("points")).intValue());
        ctx.status(200).result("Вопрос обновлен");
    }

    public static void deleteQuestion(Context ctx) throws Exception {
        if (!isAdmin(ctx)) return;
        appDao.deleteQuestion(Integer.parseInt(ctx.pathParam("id")));
        ctx.status(204);
    }

    private static boolean isAdmin(Context ctx) throws Exception {
        User u = userDao.findByUsername(ctx.attribute("username"));
        if (u == null || !"ADMIN".equals(u.role())) {
            ctx.status(403).result("Доступ запрещен");
            return false;
        }
        return true;
    }
}