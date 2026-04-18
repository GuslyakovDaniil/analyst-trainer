package com.example;

import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import com.example.controller.*;
import com.example.util.SecurityUtil;
import com.example.dao.UserDao;
import com.example.config.DatabaseConfig;
import java.sql.Connection;

public class Main {
    public static void main(String[] args) {
        System.out.println("Запуск сервера...");
        for (int i = 0; i < 15; i++) {
            try (Connection conn = DatabaseConfig.getConnection()) {
                System.out.println("БД подключена."); break;
            } catch (Exception e) {
                try { Thread.sleep(3000); } catch (Exception ignored) {}
            }
        }

        try {
            UserDao userDao = new UserDao();
            if (userDao.findByUsername("admin") == null) {
                userDao.createUser("admin", SecurityUtil.hashPassword("admin123"), "ADMIN");
            }
        } catch (Exception e) { e.printStackTrace(); }

        Javalin app = Javalin.create(config -> {
            config.staticFiles.add("/public", Location.CLASSPATH);
        }).start(8080);

        app.post("/api/register", AuthController::register);
        app.post("/api/login", AuthController::login);

        app.before("/api/protected/*", ctx -> {
            String auth = ctx.header("Authorization");
            if (auth == null || !auth.startsWith("Bearer ")) { ctx.status(401).result("Missing token"); return; }
            String user = SecurityUtil.validateToken(auth.substring(7));
            if (user == null) ctx.status(401).result("Invalid token"); else ctx.attribute("username", user);
        });

        // User Routes
        app.get("/api/protected/topics", AppController::getTopics);
        app.get("/api/protected/tests", AppController::getTests);
        app.get("/api/protected/test/{id}/start", AppController::startTest);
        app.post("/api/protected/session/{id}/submit", AppController::submitTest);

        // --- ПРОФИЛЬ И ГЕЙМИФИКАЦИЯ ---
        app.get("/api/protected/profile", AppController::getProfile);
        app.get("/api/protected/profile/session/{id}", AppController::getUserSessionDetails);
        app.get("/api/protected/leaderboard", AppController::getLeaderboard);
        app.post("/api/protected/profile/privacy", AppController::togglePrivacy);
        app.post("/api/protected/profile/update", AppController::updateProfile);

        // Admin Routes
        app.get("/api/protected/admin/users", AppController::getAdminUsers);
        app.get("/api/protected/admin/pending", AppController::getAdminPending);
        app.post("/api/protected/admin/grade", AppController::gradeAnswer);
        app.get("/api/protected/admin/all-tests", AppController::getAdminAllTests);
        app.post("/api/protected/admin/test", AppController::createTest);
        app.delete("/api/protected/admin/test/{id}", AppController::deleteTest);
        app.get("/api/protected/admin/test/{id}/questions", AppController::getTestQuestions);
        app.post("/api/protected/admin/question", AppController::addQuestion);
        app.post("/api/protected/admin/question/{id}/update", AppController::updateQuestion);
        app.delete("/api/protected/admin/question/{id}", AppController::deleteQuestion);
        app.get("/api/protected/admin/user/{id}/sessions", AppController::getAdminSessions);
        app.get("/api/protected/admin/session/{id}/details", AppController::getAdminSessionDetails);

        System.out.println("Система готова к работе.");
    }
}