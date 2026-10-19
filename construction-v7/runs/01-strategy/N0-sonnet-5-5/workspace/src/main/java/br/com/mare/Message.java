package br.com.mare;

public record Message(int id, String channel, String to, String subject, String body) {}
