package com.pousada.api;

import com.pousada.domain.Message;
import com.pousada.service.MessageService;
import io.javalin.http.Context;
import java.util.List;
import java.util.stream.Collectors;

public class MessageController {
    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    public void getAllMessages(Context ctx) {
        List<Message> messages = messageService.getAllMessages();
        List<MessageResponse> responses = messages.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        ctx.json(responses);
    }

    private MessageResponse toResponse(Message message) {
        return new MessageResponse(
                message.getId(),
                message.getChannel().toString(),
                message.getTo(),
                message.getSubject(),
                message.getBody()
        );
    }

    public static class MessageResponse {
        public final long id;
        public final String channel;
        public final String to;
        public final String subject;
        public final String body;

        public MessageResponse(long id, String channel, String to, String subject, String body) {
            this.id = id;
            this.channel = channel;
            this.to = to;
            this.subject = subject;
            this.body = body;
        }
    }
}
