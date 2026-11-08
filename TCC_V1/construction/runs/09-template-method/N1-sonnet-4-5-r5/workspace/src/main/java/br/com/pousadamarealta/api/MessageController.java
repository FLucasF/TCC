package br.com.pousadamarealta.api;

import br.com.pousadamarealta.domain.message.Message;
import br.com.pousadamarealta.domain.message.MessageRepository;
import io.javalin.http.Context;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MessageController {
    private final MessageRepository messageRepository;

    public MessageController(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public void list(Context ctx) throws SQLException {
        List<Message> messages = messageRepository.findAll();

        List<Map<String, Object>> response = messages.stream()
                .map(message -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", message.getId());
                    map.put("channel", message.getChannel().name());
                    map.put("to", message.getTo());
                    map.put("subject", message.getSubject());
                    map.put("body", message.getBody());
                    return map;
                })
                .collect(Collectors.toList());

        ctx.json(response);
    }
}
