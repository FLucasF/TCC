package pousada.service;

public record Message(int id, Channel channel, String to, String subject, String body) {
}
