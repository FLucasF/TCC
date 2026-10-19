package com.pousadamarealta.messaging;

import java.util.List;

public interface MessageRule {
    List<ProcessedMessage> apply(OutgoingMessage message, boolean isTestMode);
}
