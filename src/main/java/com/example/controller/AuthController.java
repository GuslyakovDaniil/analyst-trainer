package com.example.controller;

import io.javalin.http.Context;
import com.example.dao.UserDao;
import com.example.model.User;
import com.example.util.SecurityUtil;
import java.util.Map;

public class AuthController {
    private static final UserDao userDao = new UserDao();

    public static void register(Context ctx) throws Exception {
        Map<String, String> body = ctx.bodyAsClass(Map.class);
        String hashed = SecurityUtil.hashPassword((String) body.get("password"));
        userDao.createUser((String) body.get("username"), hashed, "USER");
        ctx.status(201).result("User created");
    }

    public static void login(Context ctx) throws Exception {
        Map<String, String> body = ctx.bodyAsClass(Map.class);
        User user = userDao.findByUsername((String) body.get("username"));
        if (user != null && SecurityUtil.checkPassword((String) body.get("password"), user.password())) {
            String token = SecurityUtil.generateToken(user.username());
            ctx.json(Map.of("token", token, "role", user.role(), "username", user.username()));
        } else {
            ctx.status(401).result("Unauthorized");
        }
    }
}