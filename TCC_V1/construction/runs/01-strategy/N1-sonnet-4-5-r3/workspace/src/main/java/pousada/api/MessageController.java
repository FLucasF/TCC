package pousada.api;

import io.javalin.http.Context;
import pousada.domain.Message;
import pousada.repository.MessageRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MessageController {

    private final MessageRepository messageRepository;

    public MessageController(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public void getMessages(Context ctx) {
        List<Message> messages = messageRepository.findAll();
        List<Map<String, Object>> response = messages.stream()
            .map(msg -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", msg.id());
                map.put("channel", msg.channel());
                map.put("to", msg.to());
                map.put("subject", msg.subject());
                map.put("body", msg.body());
                return map;
            })
            .collect(Collectors.toList());

        ctx.json(response);
    }
}
