package com.pousada.marealta.messaging.processors;

import com.pousada.marealta.messaging.MessageData;
import com.pousada.marealta.messaging.MessageProcessor;

public class ArchiveCopyProcessor implements MessageProcessor {
    private final boolean enabled;
    public static final String ARCHIVE_EMAIL = "arquivo@pousadamarealta.com.br";

    public ArchiveCopyProcessor(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public void process(MessageData message) {
        if (!enabled || message.isBlocked() || !message.shouldGenerateCopy()) {
            return;
        }
        // This processor doesn't modify the message itself
        // The chain will create a copy message after all processors run
    }
}
