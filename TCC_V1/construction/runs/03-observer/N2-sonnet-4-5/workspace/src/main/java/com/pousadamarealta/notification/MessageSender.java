package com.pousadamarealta.notification;

import com.pousadamarealta.domain.MessageChannel;

public interface MessageSender {
    void send(MessageChannel channel, String to, String subject, String body);
}
