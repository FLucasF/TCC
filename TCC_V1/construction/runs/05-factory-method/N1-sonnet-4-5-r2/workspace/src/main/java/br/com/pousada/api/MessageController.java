package br.com.pousada.api;

import br.com.pousada.domain.Message;
import br.com.pousada.repository.MessageRepository;
import io.javalin.http.Context;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessageController {
    private final MessageRepository messageRepository;

    public MessageController(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public void getMessages(Context ctx) {
        List<Message> messages = messageRepository.findAll();
        List<Map<String, Object>> response = messages.stream()
            .map(message -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", message.getId());
                map.put("channel", message.getChannel());
                map.put("to", message.getTo());
                map.put("subject", message.getSubject());
                map.put("body", message.getBody());
                return map;
            })
            .toList();

        ctx.json(response);
    }
}
