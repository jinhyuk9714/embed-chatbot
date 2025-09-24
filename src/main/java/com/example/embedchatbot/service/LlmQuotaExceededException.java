package com.example.embedchatbot.service;

public class LlmQuotaExceededException extends RuntimeException {
    public LlmQuotaExceededException(String message) { super(message); }
}