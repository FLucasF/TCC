package com.pousadamarealta.messaging;

public final class ArchiveCopyRule implements MessageRule {
    private static final String ARCHIVE_EMAIL = "arquivo@pousadamarealta.com.br";
    private static final String COPY_PREFIX = "[Cópia] ";

    @Override
    public void process(MessageContext context) {
        if (context.getStatus() == MessageStatus.BLOCKED) {
            return;
        }

        OutgoingMessage original = context.getMessage();
        OutgoingMessage copy = original
            .withTo(ARCHIVE_EMAIL)
            .prependToSubject(COPY_PREFIX);

        context.addAdditionalMessage(copy);
    }
}
