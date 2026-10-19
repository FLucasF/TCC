package br.com.mareaalta.api;

import br.com.mareaalta.service.MessageService;
import io.javalin.http.Context;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MessageController {
    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    public void getMessages(Context ctx) {
        List<Map<String, Object>> messages = messageService.getAllMessages().stream()
            .map(ApiMapper::toMessageResponse)
            .collect(Collectors.toList());
        ctx.json(messages);
    }
}
