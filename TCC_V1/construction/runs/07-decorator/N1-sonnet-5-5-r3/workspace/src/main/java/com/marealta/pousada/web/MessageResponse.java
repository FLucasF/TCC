package com.marealta.pousada.web;

import com.marealta.pousada.mail.Channel;
import com.marealta.pousada.mail.Message;
import com.marealta.pousada.mail.MessageStatus;

record MessageResponse(long id, Channel channel, String to, String subject, String body, MessageStatus status) {
    static MessageResponse of(Message m) {
        return new MessageResponse(m.id(), m.channel(), m.to(), m.subject(), m.body(), m.status());
    }
}
