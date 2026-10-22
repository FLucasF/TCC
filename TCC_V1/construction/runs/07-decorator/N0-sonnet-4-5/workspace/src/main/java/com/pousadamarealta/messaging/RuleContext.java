package com.pousadamarealta.messaging;

import java.util.ArrayList;
import java.util.List;

public class RuleContext {
    private final boolean testMode;
    private final List<OutboundMessage> additionalMessages;
    private String originalRecipient;

    public RuleContext(boolean testMode) {
        this.testMode = testMode;
        this.additionalMessages = new ArrayList<>();
    }

    public boolean isTestMode() {
        return testMode;
    }

    public List<OutboundMessage> getAdditionalMessages() {
        return additionalMessages;
    }

    public void addMessage(OutboundMessage message) {
        additionalMessages.add(message);
    }

    public String getOriginalRecipient() {
        return originalRecipient;
    }

    public void setOriginalRecipient(String originalRecipient) {
        this.originalRecipient = originalRecipient;
    }
}
