package com.pousadamarealta.messaging;

import com.pousadamarealta.domain.Message;
import com.pousadamarealta.domain.MessageStatus;

import java.util.ArrayList;
import java.util.List;

public class ArchiveCopyRule implements MessageRule {
    private static final String ARCHIVE_EMAIL = "arquivo@pousadamarealta.com.br";

    @Override
    public List<Message> apply(Message message) {
        List<Message> messages = new ArrayList<>();
        messages.add(message);

        if (message.getStatus() == MessageStatus.SENT) {
            Message copy = new Message(
                message.getChannel(),
                ARCHIVE_EMAIL,
                "[Cópia] " + message.getSubject(),
                message.getBody(),
                MessageStatus.SENT
            );
            messages.add(copy);
        }

        return messages;
    }
}
