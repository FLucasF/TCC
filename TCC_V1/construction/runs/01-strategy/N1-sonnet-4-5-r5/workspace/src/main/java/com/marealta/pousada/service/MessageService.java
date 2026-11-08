package com.marealta.pousada.service;

import com.marealta.pousada.domain.model.Message;
import com.marealta.pousada.domain.model.MessageChannel;
import com.marealta.pousada.domain.model.Reservation;
import com.marealta.pousada.repository.MessageRepository;

import java.sql.SQLException;
import java.util.List;

public class MessageService {

    private final MessageRepository messageRepository;

    public MessageService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public void sendConfirmationEmail(Reservation reservation) {
        String subject = "Reserva confirmada - Pousada Maré Alta";
        String body = buildConfirmationEmail(reservation);

        Message message = new Message(MessageChannel.EMAIL, reservation.getGuestEmail(), subject, body);

        try {
            messageRepository.save(message);
        } catch (SQLException e) {
            throw new RuntimeException("Error sending message", e);
        }
    }

    private String buildConfirmationEmail(Reservation reservation) {
        StringBuilder sb = new StringBuilder();
        sb.append("Olá, ").append(reservation.getGuestName()).append("!\n\n");
        sb.append("Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n");
        sb.append(buildReceipt(reservation));
        sb.append("\n\nA entrada é a partir das 14h, e a saída, até as 12h.\n");
        sb.append("Até breve!");
        return sb.toString();
    }

    private String buildReceipt(Reservation reservation) {
        StringBuilder sb = new StringBuilder();
        sb.append("Comprovante de reserva nº ").append(reservation.getId()).append("\n");
        sb.append("Hóspede: ").append(reservation.getGuestName())
          .append(" (CPF ").append(formatCpf(reservation.getGuestCpf())).append(")\n");
        sb.append("Quarto: ").append(reservation.getRoomNumber())
          .append(" (").append(reservation.getRoomType().name()).append(")\n");
        sb.append("Entrada: ").append(formatDate(reservation.getCheckIn()))
          .append(" | Saída: ").append(formatDate(reservation.getCheckOut()))
          .append(" | ").append(reservation.getNights())
          .append(reservation.getNights() == 1 ? " diária" : " diárias").append("\n");
        sb.append("Diárias: R$ ").append(formatMoney(reservation.getSubtotal())).append("\n");

        if (reservation.getDiscount().compareTo(java.math.BigDecimal.ZERO) > 0) {
            sb.append("Desconto: R$ ").append(formatMoney(reservation.getDiscount())).append("\n");
        }

        sb.append("Total: R$ ").append(formatMoney(reservation.getTotal()));
        return sb.toString();
    }

    private String formatCpf(String cpf) {
        if (cpf.length() == 11) {
            return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." +
                   cpf.substring(6, 9) + "-" + cpf.substring(9);
        }
        return cpf;
    }

    private String formatDate(java.time.LocalDate date) {
        return String.format("%02d/%02d/%04d", date.getDayOfMonth(), date.getMonthValue(), date.getYear());
    }

    private String formatMoney(java.math.BigDecimal amount) {
        return String.format("%,.2f", amount).replace(",", "X").replace(".", ",").replace("X", ".");
    }

    public List<Message> getAllMessages() {
        try {
            return messageRepository.findAll();
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching messages", e);
        }
    }
}
