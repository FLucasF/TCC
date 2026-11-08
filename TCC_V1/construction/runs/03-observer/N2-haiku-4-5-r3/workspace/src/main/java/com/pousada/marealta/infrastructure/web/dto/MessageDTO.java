package com.pousada.marealta.infrastructure.web.dto;

import com.pousada.marealta.domain.notification.PersistedNotification;

public class MessageDTO {
    public String id;
    public String channel;
    public String to;
    public String subject;
    public String body;

    public static MessageDTO fromDomain(PersistedNotification notification) {
        MessageDTO dto = new MessageDTO();
        dto.id = notification.id();
        dto.channel = notification.channel();
        dto.to = notification.recipient();
        dto.subject = notification.subject();
        dto.body = notification.body();
        return dto;
    }
}
