package br.com.pousadamarealta.domain;

public record Message(Long id, MessageChannel channel, String to, String subject, String body) {
    public Message(MessageChannel channel, String to, String subject, String body) {
        this(null, channel, to, subject, body);
    }
}
