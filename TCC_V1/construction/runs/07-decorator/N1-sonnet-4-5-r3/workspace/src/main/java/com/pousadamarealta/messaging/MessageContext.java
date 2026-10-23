package com.pousadamarealta.messaging;

import java.util.ArrayList;
import java.util.List;

public class MessageContext {
    private final MessageRequest original;
    private final MessageRequest current;
    private final boolean blocked;
    private final List<MessageRequest> additional;

    public MessageContext(MessageRequest original) {
        this(original, original, false, new ArrayList<>());
    }

    private MessageContext(MessageRequest original, MessageRequest current, boolean blocked, List<MessageRequest> additional) {
        this.original = original;
        this.current = current;
        this.blocked = blocked;
        this.additional = new ArrayList<>(additional);
    }

    public MessageContext withCurrent(MessageRequest newCurrent) {
        return new MessageContext(original, newCurrent, blocked, additional);
    }

    public MessageContext withBlocked(boolean newBlocked) {
        return new MessageContext(original, current, newBlocked, additional);
    }

    public MessageContext withAdditional(MessageRequest additionalMessage) {
        List<MessageRequest> newAdditional = new ArrayList<>(additional);
        newAdditional.add(additionalMessage);
        return new MessageContext(original, current, blocked, newAdditional);
    }

    public MessageRequest getOriginal() {
        return original;
    }

    public MessageRequest getCurrent() {
        return current;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public List<MessageRequest> getAdditional() {
        return new ArrayList<>(additional);
    }
}
