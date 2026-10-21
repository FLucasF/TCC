package br.com.marealta.reservas.notification;

public record Message(int id, Channel channel, String to, String subject, String body) {
}
