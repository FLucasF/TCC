package com.pousadamarealta;

import io.javalin.Javalin;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MessageController {
    private final Database database;

    public MessageController(Database database) {
        this.database = database;
    }

    public void register(Javalin app) {
        app.get("/messages", this::getMessages);
    }

    private void getMessages(io.javalin.http.Context ctx) {
        try {
            List<Message> messages = getAllMessages();
            ctx.json(messages);
        } catch (Exception e) {
            ctx.status(500);
            ctx.json(new java.util.HashMap<String, Object>() {{
                put("error", "INTERNAL_ERROR");
                put("message", "Failed to retrieve messages");
            }});
        }
    }

    private List<Message> getAllMessages() throws SQLException {
        List<Message> messages = new ArrayList<>();

        try (Connection conn = database.getConnection()) {
            var stmt = conn.prepareStatement("SELECT id, channel, to_address, subject, body FROM messages");
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                messages.add(new Message(
                    rs.getInt("id"),
                    rs.getString("channel"),
                    rs.getString("to_address"),
                    rs.getString("subject"),
                    rs.getString("body")
                ));
            }
        }

        return messages;
    }
}
