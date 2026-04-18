package com.example.model;
public record Question(Integer id, Integer testId, String type, String text, String options, String correctAnswer, int points) {}