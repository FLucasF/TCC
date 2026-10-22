package com.pousada.marealta.infrastructure.messaging;

public interface MessagePolicy {
    MessagePolicyResult apply(MessageContext context);
}

class MessagePolicyResult {
    public final String recipient;
    public final String subject;
    public final String body;
    public final boolean blocked;

    public MessagePolicyResult(String recipient, String subject, String body, boolean blocked) {
        this.recipient = recipient;
        this.subject = subject;
        this.body = body;
        this.blocked = blocked;
    }

    public static MessagePolicyResult blocked(String originalRecipient) {
        return new MessagePolicyResult(originalRecipient, "", "", true);
    }

    public static MessagePolicyResult pass(String recipient, String subject, String body) {
        return new MessagePolicyResult(recipient, subject, body, false);
    }
}

class MessageContext {
    public final String originalRecipient;
    public final String originalSubject;
    public final String originalBody;

    public MessageContext(String originalRecipient, String originalSubject, String originalBody) {
        this.originalRecipient = originalRecipient;
        this.originalSubject = originalSubject;
        this.originalBody = originalBody;
    }
}
