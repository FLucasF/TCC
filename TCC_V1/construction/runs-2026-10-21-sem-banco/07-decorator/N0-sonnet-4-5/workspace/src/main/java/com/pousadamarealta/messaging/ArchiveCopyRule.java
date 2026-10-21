package com.pousadamarealta.messaging;

import java.util.ArrayList;
import java.util.List;

public class ArchiveCopyRule implements MessageRule {
    private static final String ARCHIVE_EMAIL = "arquivo@pousadamarealta.com.br";

    @Override
    public List<OutboundMessage> apply(OutboundMessage message) {
        if (message.isBlocked()) {
            return List.of(message);
        }

        List<OutboundMessage> result = new ArrayList<>();
        result.add(message);

        OutboundMessage copy = new OutboundMessage(
            message.getChannel(),
            ARCHIVE_EMAIL,
            "[Cópia] " + message.getSubject(),
            message.getBody()
        );
        result.add(copy);

        return result;
    }
}
