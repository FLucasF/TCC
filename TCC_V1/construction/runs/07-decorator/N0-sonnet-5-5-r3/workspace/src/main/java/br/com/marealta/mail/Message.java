package br.com.marealta.mail;

public record Message(long id, String channel, String to, String subject, String body, String status) {}
