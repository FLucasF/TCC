package com.pousadamarealta.messaging;

public record OutgoingMessage(
    String to,
    String subject,
    String body
) {
    public OutgoingMessage withTo(String newTo) {
        return new OutgoingMessage(newTo, subject, body);
    }

    public OutgoingMessage withSubject(String newSubject) {
        return new OutgoingMessage(to, newSubject, body);
    }

    public OutgoingMessage withBody(String newBody) {
        return new OutgoingMessage(to, subject, newBody);
    }

    public OutgoingMessage prependToSubject(String prefix) {
        return withSubject(prefix + subject);
    }

    public OutgoingMessage prependToBody(String prefix) {
        return withBody(prefix + body);
    }
}
