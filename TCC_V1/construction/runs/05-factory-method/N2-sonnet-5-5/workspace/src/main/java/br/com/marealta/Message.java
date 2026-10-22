package br.com.marealta;

public record Message(Long id, String channel, String to, String subject, String body) {}
