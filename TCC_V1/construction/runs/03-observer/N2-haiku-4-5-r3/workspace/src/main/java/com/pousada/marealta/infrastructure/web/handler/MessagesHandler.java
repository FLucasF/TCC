package com.pousada.marealta.infrastructure.web.handler;

import com.pousada.marealta.domain.repository.NotificationRepository;
import com.pousada.marealta.infrastructure.web.dto.MessageDTO;
import io.javalin.http.Context;

import java.util.stream.Collectors;

public class MessagesHandler {
    private final NotificationRepository notificationRepository;

    public MessagesHandler(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public void listMessages(Context ctx) {
        var messages = notificationRepository.findAll()
                .stream()
                .map(MessageDTO::fromDomain)
                .collect(Collectors.toList());

        ctx.json(messages);
    }
}
