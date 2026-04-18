package com.example.model;

public record TestSession(
        Integer id,
        Integer userId,
        Integer testId,
        String status,
        int score,
        String submittedAt,
        String testName
) {}