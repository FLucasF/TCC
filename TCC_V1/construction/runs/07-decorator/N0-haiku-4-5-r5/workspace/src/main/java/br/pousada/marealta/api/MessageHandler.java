package br.pousada.marealta.api;

import io.javalin.Javalin;
import br.pousada.marealta.database.MessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageHandler {
    private static final Logger logger = LoggerFactory.getLogger(MessageHandler.class);

    public static void register(Javalin app) {
        app.get("/messages", ctx -> {
            try {
                var messages = MessageRepository.findAll();
                ctx.json(messages);
            } catch (Exception e) {
                logger.error("Error fetching messages", e);
                ctx.status(500).json(ErrorResponse.error("INTERNAL_ERROR", "Failed to fetch messages"));
            }
        });
    }
}
