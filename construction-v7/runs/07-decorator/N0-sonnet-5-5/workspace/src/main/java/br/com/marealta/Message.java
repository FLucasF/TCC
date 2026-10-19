package br.com.marealta;

public record Message(int id, String channel, String to, String subject, String body, String status) {}
