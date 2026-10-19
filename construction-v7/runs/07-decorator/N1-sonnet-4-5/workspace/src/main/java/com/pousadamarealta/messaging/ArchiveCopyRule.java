package com.pousadamarealta.messaging;

import com.pousadamarealta.domain.MessageStatus;

import java.util.Collections;
import java.util.List;

public class ArchiveCopyRule implements MessageRule {
    private static final String ARCHIVE_EMAIL = "arquivo@pousadamarealta.com.br";

    @Override
    public List<ProcessedMessage> apply(OutgoingMessage message, boolean isTestMode) {
        if (isTestMode) {
            return Collections.emptyList();
        }

        String archiveSubject = "[Cópia] " + message.getSubject();

        return List.of(new ProcessedMessage(
            message.getChannel(),
            ARCHIVE_EMAIL,
            archiveSubject,
            message.getBody(),
            MessageStatus.SENT
        ));
    }
}
