package com.pousada.service;

import com.pousada.db.Database;
import com.pousada.entity.Reservation;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MessageService {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public void sendConfirmationEmail(Reservation res, BigDecimal amountPaid) throws SQLException {
        String voucherText = generateVoucher(res);

        String emailBody = String.format("""
            Olá, %s!

            Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:

            %s

            A entrada é a partir das 14h, e a saída, até as 12h.
            Até breve!
            """, res.guestName, voucherText);

        storeMessage("EMAIL", res.guestEmail, "Reserva confirmada - Pousada Maré Alta", emailBody);
    }

    public String generateVoucher(Reservation res) {
        StringBuilder sb = new StringBuilder();
        sb.append("Comprovante de reserva nº ").append(res.id).append("\n");
        sb.append("Hóspede: ").append(res.guestName).append(" (CPF ").append(formatCpf(res.guestCpf)).append(")\n");
        sb.append("Quarto: ").append(res.roomNumber).append(" (").append(res.roomType).append(")\n");
        sb.append("Entrada: ").append(res.checkIn.format(DATE_FORMATTER));
        sb.append(" | Saída: ").append(res.checkOut.format(DATE_FORMATTER));
        sb.append(" | ").append(res.nights).append(" ").append(res.nights == 1 ? "diária" : "diárias").append("\n");
        sb.append("Diárias: R$ ").append(formatMoney(res.subtotal)).append("\n");

        if (res.discount.signum() > 0) {
            sb.append("Desconto: R$ ").append(formatMoney(res.discount)).append("\n");
        }

        sb.append("Total: R$ ").append(formatMoney(res.total));

        return sb.toString();
    }

    private void storeMessage(String channel, String to, String subject, String body) throws SQLException {
        String sql = "INSERT INTO messages (channel, destination, subject, body) VALUES (?, ?, ?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, channel);
            pstmt.setString(2, to);
            pstmt.setString(3, subject);
            pstmt.setString(4, body);
            pstmt.executeUpdate();
        }
    }

    public List<Map<String, Object>> getMessages() throws SQLException {
        List<Map<String, Object>> messages = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement()) {
            var rs = stmt.executeQuery("""
                SELECT id, channel, destination, subject, body FROM messages
                ORDER BY created_at ASC
                """);

            while (rs.next()) {
                Map<String, Object> msg = new LinkedHashMap<>();
                msg.put("id", rs.getInt("id"));
                msg.put("channel", rs.getString("channel"));
                msg.put("to", rs.getString("destination"));
                msg.put("subject", rs.getString("subject"));
                msg.put("body", rs.getString("body"));
                messages.add(msg);
            }
        }
        return messages;
    }

    private String formatCpf(String cpf) {
        if (cpf == null || cpf.length() != 11) return cpf;
        return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." + cpf.substring(6, 9) + "-" + cpf.substring(9);
    }

    private String formatMoney(BigDecimal value) {
        return String.format(java.util.Locale.GERMANY, "%.2f", value);
    }
}
