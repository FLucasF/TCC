package com.pousadamarealta.messaging;

public interface MessageRule {
    MessageContext apply(MessageContext context);
}
