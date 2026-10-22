package com.pousadamarealta.service;

import com.pousadamarealta.db.Database;
import com.pousadamarealta.model.Message;
import com.pousadamarealta.model.MessageStatus;
import com.pousadamarealta.util.EmailValidator;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MessageService {
    private final Database database;
    private final boolean testMode;
    private int messageId = 0;

    public MessageService(Database database) {
        this.database = database;
        this.testMode = "true".equalsIgnoreCase(System.getenv("HOTEL_TEST_MODE"));
        initializeMessageId();
    }

    private void initializeMessageId() {
        try (Connection conn = database.getConnection()) {
            String sql = "SELECT COALESCE(MAX(id), 0) FROM messages";
            try (Statement stmt = conn.createStatement()) {
                ResultSet rs = stmt.executeQuery(sql);
                if (rs.next()) {
                    messageId = rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendConfirmationEmail(String guestName, String guestEmail, String receipt) {
        String subject = "Reserva confirmada - Pousada Maré Alta";
        String body = "Olá, " + guestName + "!\n\n" +
                "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n" +
                receipt + "\n\n" +
                "A entrada é a partir das 14h, e a saída, até as 12h.\n" +
                "Até breve!";

        sendMessage("EMAIL", guestEmail, subject, body);
    }

    public void sendMessage(String channel, String to, String subject, String body) {
        String effectiveTo = to;
        String effectiveSubject = subject;
        String effectiveBody = body;
        MessageStatus status = MessageStatus.SENT;

        if (!EmailValidator.isValid(to)) {
            effectiveSubject = subject;
            effectiveBody = body;
            status = MessageStatus.BLOCKED;
            saveMessage("EMAIL", to, effectiveSubject, effectiveBody, status);
            return;
        }

        if (testMode) {
            effectiveTo = "testes@pousadamarealta.com.br";
            effectiveSubject = "[TESTE] " + subject;
            effectiveBody = "Destinatário original: " + to + "\n" + body;
        } else {
            effectiveTo = to;
        }

        saveMessage("EMAIL", effectiveTo, effectiveSubject, effectiveBody, status);

        if (!testMode && status == MessageStatus.SENT) {
            String copyTo = "arquivo@pousadamarealta.com.br";
            String copySubject = "[Cópia] " + subject;
            saveMessage("EMAIL", copyTo, copySubject, body, MessageStatus.SENT);
        }
    }

    private void saveMessage(String channel, String to, String subject, String body, MessageStatus status) {
        try (Connection conn = database.getConnection()) {
            String sql = "INSERT INTO messages (channel, \"to\", subject, body, status) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, channel);
                stmt.setString(2, to);
                stmt.setString(3, subject);
                stmt.setString(4, body);
                stmt.setString(5, status.name());
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Message> getAllMessages() {
        List<Message> messages = new ArrayList<>();
        try (Connection conn = database.getConnection()) {
            String sql = "SELECT id, channel, \"to\", subject, body, status FROM messages ORDER BY id";
            try (Statement stmt = conn.createStatement()) {
                ResultSet rs = stmt.executeQuery(sql);
                while (rs.next()) {
                    Message msg = new Message(
                            rs.getInt("id"),
                            rs.getString("channel"),
                            rs.getString("to"),
                            rs.getString("subject"),
                            rs.getString("body"),
                            MessageStatus.valueOf(rs.getString("status"))
                    );
                    messages.add(msg);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return messages;
    }
}
