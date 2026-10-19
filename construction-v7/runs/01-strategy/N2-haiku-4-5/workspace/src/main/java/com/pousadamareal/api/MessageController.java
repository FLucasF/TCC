package com.pousadamareal.api;

import com.pousadamareal.domain.Message;
import com.pousadamareal.service.DataStore;
import io.javalin.http.Context;

import java.util.List;
import java.util.stream.Collectors;

public class MessageController {
    private final DataStore dataStore;

    public MessageController(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    public void getMessages(Context ctx) {
        List<MessageResponse> messages = dataStore.getAllMessages().stream()
                .map(m -> new MessageResponse(m.id, m.channel, m.to, m.subject, m.body))
                .collect(Collectors.toList());

        ctx.json(messages);
    }

    public record MessageResponse(long id, String channel, String to, String subject, String body) {}
}
