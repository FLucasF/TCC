package com.pousada.messaging;

import com.pousada.domain.Message;
import com.pousada.domain.MessageStatus;

public class FileCopyPolicy implements MessagePolicy {
    private static final String FILE_EMAIL = "arquivo@pousadamarealta.com.br";
    private static final String COPY_PREFIX = "[Cópia] ";

    @Override
    public void apply(PolicyResult result) {
        if (result.isBlocked()) {
            return;
        }

        Message original = result.getMessage();
        Message copy = new Message(
            original.getChannel(),
            FILE_EMAIL,
            COPY_PREFIX + original.getSubject(),
            original.getBody(),
            MessageStatus.SENT
        );

        result.addMessage(copy);
    }
}
