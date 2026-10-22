package com.marealta.mail;

/** Keeps a message and its derived copies together in the list, even with concurrent requests. */
public final class Serialized implements Outbox {
    private final Outbox next;

    public Serialized(Outbox next) {
        this.next = next;
    }

    @Override
    public synchronized Message send(Message message) {
        return next.send(message);
    }
}
