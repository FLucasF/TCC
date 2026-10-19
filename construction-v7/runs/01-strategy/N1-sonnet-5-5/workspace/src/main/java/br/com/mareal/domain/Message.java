package br.com.mareal.domain;

public record Message(int id, String channel, String to, String subject, String body) {}
