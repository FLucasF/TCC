package pousada.domain;

public record Message(Long id, MessageChannel channel, String to, String subject, String body) {
}
