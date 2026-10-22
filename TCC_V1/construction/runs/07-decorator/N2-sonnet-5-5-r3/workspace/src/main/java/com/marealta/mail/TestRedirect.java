package com.marealta.mail;

/** Sends everything to a test mailbox instead of the real recipient. */
public final class TestRedirect implements Outbox {
    private final Outbox next;
    private final String testAddress;

    public TestRedirect(Outbox next, String testAddress) {
        this.next = next;
        this.testAddress = testAddress;
    }

    @Override
    public Message send(Message message) {
        if (message.status() == MessageStatus.BLOCKED) {
            return next.send(message);
        }
        return next.send(message.redirectedTo(testAddress,
                "[TESTE] " + message.subject(),
                "Destinatário original: " + message.to() + "\n" + message.body()));
    }
}
