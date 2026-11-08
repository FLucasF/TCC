package com.pousadamareaita.service;

import com.pousadamareaita.model.Message;
import com.pousadamareaita.model.Reservation;
import com.pousadamareaita.repository.MessageRepository;

import java.time.format.DateTimeFormatter;

public class EmailService {
    private static final MessageRepository messageRepo = new MessageRepository();
    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static void sendConfirmationEmail(Reservation reservation) {
        String cpfFormatted = formatCpf(reservation.getGuestCpf());
        String checkInFormatted = reservation.getCheckIn().format(dateFormatter);
        String checkOutFormatted = reservation.getCheckOut().format(dateFormatter);
        String nightsText = reservation.getNights() == 1 ? "1 diária" : reservation.getNights() + " diárias";

        StringBuilder body = new StringBuilder();
        body.append("Olá, ").append(reservation.getGuestName()).append("!\n\n");
        body.append("Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n");
        body.append("Comprovante de reserva nº ").append(reservation.getId()).append("\n");
        body.append("Hóspede: ").append(reservation.getGuestName()).append(" (CPF ").append(cpfFormatted).append(")\n");
        body.append("Quarto: ").append(reservation.getRoomNumber()).append(" (").append(getQuartoType(reservation.getRoomNumber())).append(")\n");
        body.append("Entrada: ").append(checkInFormatted).append(" | Saída: ").append(checkOutFormatted).append(" | ").append(nightsText).append("\n");
        body.append("Diárias: ").append(formatCurrency(reservation.getSubtotal())).append("\n");

        if (reservation.getDiscount().compareTo(java.math.BigDecimal.ZERO) > 0) {
            body.append("Desconto: ").append(formatCurrency(reservation.getDiscount())).append("\n");
        }

        body.append("Total: ").append(formatCurrency(reservation.getTotal())).append("\n\n");
        body.append("A entrada é a partir das 14h, e a saída, até as 12h.\n");
        body.append("Até breve!");

        Message message = new Message(
            "EMAIL",
            reservation.getGuestEmail(),
            "Reserva confirmada - Pousada Maré Alta",
            body.toString()
        );

        messageRepo.save(message);
    }

    private static String formatCpf(String cpf) {
        if (cpf == null || cpf.length() != 11) {
            return cpf;
        }
        return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." +
               cpf.substring(6, 9) + "-" + cpf.substring(9);
    }

    private static String getQuartoType(int roomNumber) {
        return switch (roomNumber) {
            case 101, 102 -> "STANDARD";
            case 201, 202 -> "DELUXE";
            case 301 -> "SUITE";
            default -> "UNKNOWN";
        };
    }

    private static String formatCurrency(java.math.BigDecimal value) {
        return "R$ " + String.format("%.2f", value).replace(".", ",");
    }
}
