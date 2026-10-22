package com.pousada.marealta.service;

import com.pousada.marealta.db.Database;
import com.pousada.marealta.model.Message;
import com.pousada.marealta.util.EmailValidator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class MessageService {
    private static final String ARCHIVE_EMAIL = "arquivo@pousadamarealta.com.br";
    private static final String TEST_EMAIL = "testes@pousadamarealta.com.br";
    private static final boolean TEST_MODE = "true".equalsIgnoreCase(System.getenv("HOTEL_TEST_MODE"));

    public static void sendMessage(String to, String subject, String body) throws SQLException {
        Message message = new Message();
        message.channel = "EMAIL";
        message.subject = subject;
        message.body = body;

        if (!EmailValidator.isValid(to)) {
            message.to = to;
            message.status = "BLOCKED";
            saveMessage(message);
            return;
        }

        String finalTo = to;
        String finalSubject = subject;
        String finalBody = body;

        if (TEST_MODE) {
            finalSubject = "[TESTE] " + subject;
            finalBody = "Destinatário original: " + to + "\n\n" + body;
            finalTo = TEST_EMAIL;
        }

        message.to = finalTo;
        message.status = "SENT";
        saveMessage(message);

        if (!TEST_MODE && EmailValidator.isValid(to)) {
            Message archive = new Message();
            archive.channel = "EMAIL";
            archive.to = ARCHIVE_EMAIL;
            archive.subject = "[Cópia] " + subject;
            archive.body = body;
            archive.status = "SENT";
            saveMessage(archive);
        }
    }

    private static void saveMessage(Message message) throws SQLException {
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO messages (channel, \"to\", subject, body, status) VALUES (?, ?, ?, ?, ?)",
                     Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, message.channel);
            stmt.setString(2, message.to);
            stmt.setString(3, message.subject);
            stmt.setString(4, message.body);
            stmt.setString(5, message.status);

            stmt.executeUpdate();

            var keys = stmt.getGeneratedKeys();
            if (keys.next()) {
                message.id = keys.getInt(1);
            }
        }
    }

    public static List<Message> getMessages() throws SQLException {
        List<Message> messages = new ArrayList<>();

        try (Connection conn = Database.getConnection();
             var stmt = conn.createStatement()) {

            var result = stmt.executeQuery("SELECT id, channel, \"to\", subject, body, status FROM messages ORDER BY id");

            while (result.next()) {
                messages.add(new Message(
                        result.getInt("id"),
                        result.getString("channel"),
                        result.getString("to"),
                        result.getString("subject"),
                        result.getString("body"),
                        result.getString("status")
                ));
            }
        }

        return messages;
    }
}
