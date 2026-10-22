package br.com.pousadamareal.api.handlers;

import br.com.pousadamareal.domain.Message;
import br.com.pousadamareal.repository.MessageRepository;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.javalin.http.Context;

import java.sql.SQLException;
import java.util.List;

public class MessageHandler {
    private final MessageRepository messageRepository;

    public MessageHandler(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public void getAllMessages(Context ctx) throws SQLException {
        List<Message> messages = messageRepository.findAll();
        JsonArray jsonArray = new JsonArray();

        for (Message message : messages) {
            JsonObject json = new JsonObject();
            json.addProperty("id", message.getId());
            json.addProperty("channel", message.getChannel().name());
            json.addProperty("to", message.getTo());
            json.addProperty("subject", message.getSubject());
            json.addProperty("body", message.getBody());
            jsonArray.add(json);
        }

        ctx.json(jsonArray);
    }
}
