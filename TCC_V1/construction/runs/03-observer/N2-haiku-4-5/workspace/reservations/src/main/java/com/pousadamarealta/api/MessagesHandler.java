package com.pousadamarealta.api;

import com.pousadamarealta.database.MessageDAO;
import com.pousadamarealta.domain.Message;
import io.javalin.http.Context;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MessagesHandler {
    public static void getMessages(Context ctx) throws Exception {
        List<Message> messages = MessageDAO.getInstance().getAllMessages();
        List<Map<String, Object>> response = new ArrayList<>();

        for (Message msg : messages) {
            response.add(Map.of(
                    "id", msg.getId(),
                    "channel", msg.getChannel().getCode(),
                    "to", msg.getTo(),
                    "subject", msg.getSubject(),
                    "body", msg.getBody()
            ));
        }

        ctx.json(response);
    }
}
