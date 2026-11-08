package br.marealta.api;

import br.marealta.db.MessageDAO;
import io.javalin.http.Context;

public class MessageController {
    private final MessageDAO messageDAO;

    public MessageController(MessageDAO messageDAO) {
        this.messageDAO = messageDAO;
    }

    public void getMessages(Context ctx) {
        ctx.json(messageDAO.findAll().stream()
            .map(msg -> new Object() {
                public final long id = msg.getId();
                public final String channel = msg.getChannel().name();
                public final String to = msg.getTo();
                public final String subject = msg.getSubject();
                public final String body = msg.getBody();
            })
            .toList());
    }
}
