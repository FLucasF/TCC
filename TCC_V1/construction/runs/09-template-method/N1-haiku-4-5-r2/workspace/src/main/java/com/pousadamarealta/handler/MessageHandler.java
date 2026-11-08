package com.pousadamarealta.handler;

import io.javalin.http.Context;
import com.pousadamarealta.infrastructure.Database;
import com.pousadamarealta.repository.MessageRepository;
import com.pousadamarealta.model.Message;
import java.util.List;
import java.util.stream.Collectors;

public class MessageHandler {
    private MessageRepository messageRepository;

    public MessageHandler(Database database) {
        this.messageRepository = new MessageRepository(database);
    }

    public void listMessages(Context ctx) throws Exception {
        List<Message> messages = messageRepository.findAll();
        List<MessageDto> dtos = messages.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        ctx.json(dtos);
    }

    private MessageDto toDto(Message message) {
        return new MessageDto(
            message.getId(),
            message.getChannel(),
            message.getTo(),
            message.getSubject(),
            message.getBody()
        );
    }

    public static class MessageDto {
        public Long id;
        public String channel;
        public String to;
        public String subject;
        public String body;

        public MessageDto(Long id, String channel, String to, String subject, String body) {
            this.id = id;
            this.channel = channel;
            this.to = to;
            this.subject = subject;
            this.body = body;
        }
    }
}
