package com.pousadamarealta.service;

import com.pousadamarealta.database.Database;
import com.pousadamarealta.model.Message;
import com.pousadamarealta.util.ValidationUtil;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MessageService {
    private static final String ARCHIVE_EMAIL = "arquivo@pousadamarealta.com.br";
    private static final String TEST_EMAIL = "testes@pousadamarealta.com.br";
    private static final boolean TEST_MODE = "true".equalsIgnoreCase(System.getenv("HOTEL_TEST_MODE"));

    public static void sendReservationConfirmation(int reservationId, String guestName, String guestEmail, String receipt) throws SQLException {
        String subject = "Reserva confirmada - Pousada Maré Alta";
        String body = "Olá, " + guestName + "!\n\n" +
                      "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n" +
                      receipt + "\n\n" +
                      "A entrada é a partir das 14h, e a saída, até as 12h.\n" +
                      "Até breve!";

        sendMessage("EMAIL", guestEmail, subject, body);
    }

    public static void sendMessage(String channel, String to, String subject, String body) throws SQLException {
        if (!ValidationUtil.isValidEmail(to)) {
            insertMessage(channel, to, subject, body, "BLOCKED");
            return;
        }

        String finalTo = to;
        String finalSubject = subject;
        String finalBody = body;

        if (TEST_MODE) {
            finalBody = "Destinatário original: " + to + "\n" + body;
            finalSubject = "[TESTE] " + subject;
            finalTo = TEST_EMAIL;
        }

        insertMessage(channel, finalTo, finalSubject, finalBody, "SENT");

        if (!TEST_MODE && !finalTo.equals(to)) {
            String archiveSubject = "[Cópia] " + subject;
            insertMessage(channel, ARCHIVE_EMAIL, archiveSubject, body, "SENT");
        }
    }

    public static List<Message> getAllMessages() throws SQLException {
        List<Message> messages = new ArrayList<>();
        try (Connection conn = Database.getConnection()) {
            var rs = conn.createStatement().executeQuery(
                "SELECT id, channel, \"to\", subject, body, status FROM messages ORDER BY id");

            while (rs.next()) {
                messages.add(rowToMessage(rs));
            }
        }
        return messages;
    }

    private static void insertMessage(String channel, String to, String subject, String body, String status) throws SQLException {
        try (Connection conn = Database.getConnection()) {
            var stmt = conn.prepareStatement(
                "INSERT INTO messages (channel, \"to\", subject, body, status) VALUES (?, ?, ?, ?, ?)");
            stmt.setString(1, channel);
            stmt.setString(2, to);
            stmt.setString(3, subject);
            stmt.setString(4, body);
            stmt.setString(5, status);
            stmt.executeUpdate();
        }
    }

    private static Message rowToMessage(ResultSet rs) throws SQLException {
        return new Message(
            rs.getInt("id"),
            rs.getString("channel"),
            rs.getString("to"),
            rs.getString("subject"),
            rs.getString("body"),
            rs.getString("status")
        );
    }
}
