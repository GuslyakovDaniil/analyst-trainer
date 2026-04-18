package com.example.model;
public record TestAnswer(Integer id, Integer sessionId, Integer questionId, String userAnswer, boolean isCorrect, int earnedPoints) {}