package br.com.marealta.reservas.notification;

public record Message(long id, String channel, String to, String subject, String body) {
}
