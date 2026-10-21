package com.pousadamarealta.messaging;

import java.util.List;

public interface MessageRule {
    List<OutboundMessage> apply(OutboundMessage message);
}
