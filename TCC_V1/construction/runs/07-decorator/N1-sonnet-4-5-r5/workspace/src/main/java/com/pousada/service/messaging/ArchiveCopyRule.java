package com.pousada.service.messaging;

public class ArchiveCopyRule implements MessageRule {
    private static final String ARCHIVE_EMAIL = "arquivo@pousadamarealta.com.br";

    @Override
    public ProcessingResult process(OutgoingMessage message) {
        ProcessingResult result = new ProcessingResult();

        if (message.isBlocked()) {
            return result;
        }

        OutgoingMessage copy = new OutgoingMessage(
            message.getChannel(),
            ARCHIVE_EMAIL,
            "[Cópia] " + message.getSubject(),
            message.getBody()
        );

        result.addMessage(copy);
        return result;
    }
}
