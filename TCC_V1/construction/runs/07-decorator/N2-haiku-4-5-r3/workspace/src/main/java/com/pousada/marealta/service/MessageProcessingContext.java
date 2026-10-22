package com.pousada.marealta.service;

import com.pousada.marealta.domain.Message;
import java.util.ArrayList;
import java.util.List;

public class MessageProcessingContext {
    private final boolean testMode;
    private final boolean copyToArchiveEnabled;
    private final List<Message> messagesGenerated;

    public MessageProcessingContext(boolean testMode, boolean copyToArchiveEnabled) {
        this.testMode = testMode;
        this.copyToArchiveEnabled = copyToArchiveEnabled;
        this.messagesGenerated = new ArrayList<>();
    }

    public boolean isTestMode() {
        return testMode;
    }

    public boolean isCopyToArchiveEnabled() {
        return copyToArchiveEnabled;
    }

    public void addGeneratedMessage(Message message) {
        messagesGenerated.add(message);
    }

    public List<Message> getGeneratedMessages() {
        return messagesGenerated;
    }
}
