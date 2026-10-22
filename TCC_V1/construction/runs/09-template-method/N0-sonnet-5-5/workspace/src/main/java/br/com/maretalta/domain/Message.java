package br.com.maretalta.domain;

public record Message(long id, String channel, String to, String subject, String body) {}
