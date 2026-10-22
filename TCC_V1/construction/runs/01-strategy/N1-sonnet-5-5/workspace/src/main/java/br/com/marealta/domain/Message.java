package br.com.marealta.domain;

public record Message(long id, String channel, String to, String subject, String body) {
}
