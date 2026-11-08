package com.pousada.service;

import com.pousada.domain.Message;
import com.pousada.domain.Reservation;
import com.pousada.repository.MessageRepository;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class MessageService {
    private final MessageRepository messageRepository;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final NumberFormat MONEY_FORMAT = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    public MessageService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public void sendConfirmationEmail(Reservation reservation) {
        String receipt = buildReceipt(reservation);

        String body = String.format("""
            Olá, %s!

            Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:

            %s

            A entrada é a partir das 14h, e a saída, até as 12h.
            Até breve!
            """, reservation.getGuestName(), receipt);

        Message message = new Message(
            "EMAIL",
            reservation.getGuestEmail(),
            "Reserva confirmada - Pousada Maré Alta",
            body
        );

        messageRepository.save(message);
    }

    public String buildReceipt(Reservation reservation) {
        StringBuilder receipt = new StringBuilder();
        receipt.append("Comprovante de reserva nº ").append(reservation.getId()).append("\n");
        receipt.append("Hóspede: ").append(reservation.getGuestName())
               .append(" (CPF ").append(formatCpf(reservation.getGuestCpf())).append(")\n");

        String roomType = getRoomType(reservation.getRoomNumber());
        receipt.append("Quarto: ").append(reservation.getRoomNumber())
               .append(" (").append(roomType).append(")\n");

        receipt.append("Entrada: ").append(reservation.getCheckIn().format(DATE_FORMAT))
               .append(" | Saída: ").append(reservation.getCheckOut().format(DATE_FORMAT))
               .append(" | ").append(reservation.getNights())
               .append(reservation.getNights() == 1 ? " diária" : " diárias").append("\n");

        receipt.append("Diárias: ").append(MONEY_FORMAT.format(reservation.getSubtotal())).append("\n");

        if (reservation.getDiscount().compareTo(java.math.BigDecimal.ZERO) > 0) {
            receipt.append("Desconto: ").append(MONEY_FORMAT.format(reservation.getDiscount())).append("\n");
        }

        receipt.append("Total: ").append(MONEY_FORMAT.format(reservation.getTotal()));

        return receipt.toString();
    }

    private String formatCpf(String cpf) {
        if (cpf.length() == 11) {
            return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." +
                   cpf.substring(6, 9) + "-" + cpf.substring(9);
        }
        return cpf;
    }

    private String getRoomType(String roomNumber) {
        return switch (roomNumber) {
            case "101", "102" -> "STANDARD";
            case "201", "202" -> "DELUXE";
            case "301" -> "SUITE";
            default -> "";
        };
    }

    public List<Message> findAll() {
        return messageRepository.findAll();
    }
}
