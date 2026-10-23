package com.pousadamarealta.messaging;

import com.pousadamarealta.model.Message;
import java.util.ArrayList;
import java.util.List;

public class MessageRuleContext {
    private final Message originalMessage;
    private Message currentMessage;
    private boolean blocked;
    private final List<Message> additionalMessages;

    public MessageRuleContext(Message message) {
        this.originalMessage = message;
        this.currentMessage = new Message(
            message.getChannel(),
            message.getTo(),
            message.getSubject(),
            message.getBody()
        );
        this.blocked = false;
        this.additionalMessages = new ArrayList<>();
    }

    public Message getOriginalMessage() {
        return originalMessage;
    }

    public Message getCurrentMessage() {
        return currentMessage;
    }

    public void setCurrentMessage(Message message) {
        this.currentMessage = message;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public void block() {
        this.blocked = true;
    }

    public List<Message> getAdditionalMessages() {
        return additionalMessages;
    }

    public void addMessage(Message message) {
        additionalMessages.add(message);
    }
}
