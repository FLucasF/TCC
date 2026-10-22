package com.pousada.marealta.messaging;

import com.pousada.marealta.domain.Message;

public interface MessageProcessingRule {
    void process(Message message, MessageProcessingContext context);
}
