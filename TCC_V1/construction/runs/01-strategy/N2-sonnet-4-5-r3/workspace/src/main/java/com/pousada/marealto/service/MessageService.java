package com.pousada.marealto.service;

import com.pousada.marealto.domain.Reservation;
import com.pousada.marealto.messaging.Message;
import com.pousada.marealto.messaging.MessageChannel;
import com.pousada.marealto.repository.MessageRepository;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class MessageService {
    private final MessageRepository messageRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Locale PT_BR = new Locale("pt", "BR");

    public MessageService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public void sendConfirmationEmail(Reservation reservation) {
        String document = generateDocument(reservation);

        String body = String.format("""
            Olá, %s!

            Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:

            %s

            A entrada é a partir das 14h, e a saída, até as 12h.
            Até breve!
            """, reservation.getGuestName(), document);

        Message message = new Message(
            MessageChannel.EMAIL,
            reservation.getGuestEmail(),
            "Reserva confirmada - Pousada Maré Alta",
            body
        );

        messageRepository.save(message);
    }

    public String generateDocument(Reservation reservation) {
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(PT_BR);

        StringBuilder doc = new StringBuilder();
        doc.append("Comprovante de reserva nº ").append(reservation.getId()).append("\n");
        doc.append("Hóspede: ").append(reservation.getGuestName())
           .append(" (CPF ").append(formatCpf(reservation.getGuestCpf())).append(")\n");

        String roomType = getRoomType(reservation.getRoomNumber());
        doc.append("Quarto: ").append(reservation.getRoomNumber())
           .append(" (").append(roomType).append(")\n");

        doc.append("Entrada: ").append(reservation.getCheckIn().format(DATE_FORMATTER))
           .append(" | Saída: ").append(reservation.getCheckOut().format(DATE_FORMATTER))
           .append(" | ").append(reservation.getNights())
           .append(reservation.getNights() == 1 ? " diária" : " diárias").append("\n");

        doc.append("Diárias: ").append(currencyFormat.format(reservation.getSubtotal())).append("\n");

        if (reservation.getDiscount().compareTo(java.math.BigDecimal.ZERO) > 0) {
            doc.append("Desconto: ").append(currencyFormat.format(reservation.getDiscount())).append("\n");
        }

        doc.append("Total: ").append(currencyFormat.format(reservation.getTotal()));

        return doc.toString();
    }

    private String formatCpf(String cpf) {
        if (cpf == null || cpf.length() != 11) {
            return cpf;
        }
        return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." +
               cpf.substring(6, 9) + "-" + cpf.substring(9);
    }

    private String getRoomType(String roomNumber) {
        return switch (roomNumber) {
            case "101", "102" -> "STANDARD";
            case "201", "202" -> "DELUXE";
            case "301" -> "SUITE";
            default -> "UNKNOWN";
        };
    }

    public List<Message> getAllMessages() {
        return messageRepository.findAll();
    }
}
