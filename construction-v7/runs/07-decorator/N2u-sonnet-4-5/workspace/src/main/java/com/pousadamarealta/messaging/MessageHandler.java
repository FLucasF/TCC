package com.pousadamarealta.messaging;

import java.util.List;

public interface MessageHandler {
    void handle(OutgoingMessage message, List<Message> sentMessages);
}
