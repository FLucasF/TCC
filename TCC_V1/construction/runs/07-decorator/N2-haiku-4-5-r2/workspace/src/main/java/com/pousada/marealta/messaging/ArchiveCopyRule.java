package com.pousada.marealta.messaging;

import com.pousada.marealta.config.AppConfig;
import com.pousada.marealta.domain.Message;

public class ArchiveCopyRule implements MessageProcessingRule {
    private static final String ARCHIVE_EMAIL = "arquivo@pousadamarealta.com.br";
    private static final String COPY_PREFIX = "[Cópia] ";
    private boolean enabled;

    public ArchiveCopyRule(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public void process(Message message, MessageProcessingContext context) {
        if (!enabled || AppConfig.isTestMode() || message.getStatus() == Message.Status.BLOCKED) {
            return;
        }

        String copySubject = COPY_PREFIX + message.getSubject();
        Message copy = new Message(message.getChannel(), ARCHIVE_EMAIL, copySubject, message.getBody());
        copy.setStatus(Message.Status.SENT);
        context.addMessage(copy);
    }
}
