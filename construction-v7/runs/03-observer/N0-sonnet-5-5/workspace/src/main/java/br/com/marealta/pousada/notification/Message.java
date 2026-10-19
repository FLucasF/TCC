package br.com.marealta.pousada.notification;

public record Message(long id, Channel channel, String to, String subject, String body) {
}
