package br.com.pousadamarealta.api;

import br.com.pousadamarealta.api.dto.MessageDTO;
import br.com.pousadamarealta.repository.MessageRepository;
import io.javalin.http.Context;

import java.util.List;

public class MessageController {
    private final MessageRepository messageRepository;

    public MessageController(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public void getMessages(Context ctx) {
        List<MessageDTO> messages = messageRepository.findAll().stream()
            .map(MessageDTO::from)
            .toList();
        ctx.json(messages);
    }
}
