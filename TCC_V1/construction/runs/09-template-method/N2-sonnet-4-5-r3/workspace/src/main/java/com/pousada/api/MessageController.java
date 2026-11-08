package com.pousada.api;

import com.pousada.api.dto.MessageResponse;
import com.pousada.domain.Message;
import com.pousada.repository.MessageRepository;
import io.javalin.http.Context;

import java.util.List;

public class MessageController {
    private final MessageRepository messageRepository;

    public MessageController(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public void getMessages(Context ctx) {
        List<Message> messages = messageRepository.findAll();
        List<MessageResponse> response = messages.stream()
            .map(msg -> new MessageResponse(
                msg.getId(),
                msg.getChannel(),
                msg.getTo(),
                msg.getSubject(),
                msg.getBody()
            ))
            .toList();
        ctx.json(response);
    }
}
