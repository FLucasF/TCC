package br.com.marealta.reservas.domain;

public record Message(int id, String channel, String to, String subject, String body) {
}
