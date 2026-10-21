package br.com.pousadamarealta.reservas.messaging;

public record Message(long id, Channel channel, String to, String subject, String body, MessageStatus status) {
}
