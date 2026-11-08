package pousada.domain;

public record Message(long id, Channel channel, String to, String subject, String body) {
}
