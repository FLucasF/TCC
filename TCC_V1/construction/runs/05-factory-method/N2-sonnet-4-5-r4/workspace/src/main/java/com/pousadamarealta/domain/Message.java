package com.pousadamarealta.domain;

public record Message(
    long id,
    String channel,
    String to,
    String subject,
    String body
) {
    public Message(String channel, String to, String subject, String body) {
        this(0, channel, to, subject, body);
    }

    public Message withId(long id) {
        return new Message(id, channel, to, subject, body);
    }
}
