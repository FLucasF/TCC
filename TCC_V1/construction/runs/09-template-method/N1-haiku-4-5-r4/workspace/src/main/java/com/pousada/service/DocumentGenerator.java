package com.pousada.service;

import com.pousada.model.Reservation;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

public class DocumentGenerator {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static String generateReceipt(Reservation reservation, String roomType) {
        StringBuilder sb = new StringBuilder();
        sb.append("Comprovante de reserva nº ").append(reservation.getId()).append("\n");
        sb.append("Hóspede: ").append(reservation.getGuestName()).append(" (CPF ").append(reservation.getGuestCpf()).append(")\n");
        sb.append("Quarto: ").append(reservation.getRoomNumber()).append(" (").append(roomType).append(")\n");
        sb.append("Entrada: ").append(reservation.getCheckIn().format(DATE_FORMATTER));
        sb.append(" | Saída: ").append(reservation.getCheckOut().format(DATE_FORMATTER));
        sb.append(" | ").append(reservation.getNights());
        if (reservation.getNights() == 1) {
            sb.append(" diária\n");
        } else {
            sb.append(" diárias\n");
        }
        sb.append("Diárias: ").append(formatCurrency(reservation.getSubtotal())).append("\n");

        if (reservation.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Desconto: ").append(formatCurrency(reservation.getDiscount())).append("\n");
        }

        sb.append("Total: ").append(formatCurrency(reservation.getTotal())).append("\n");

        return sb.toString();
    }

    public static String generateConfirmationEmail(Reservation reservation, String roomType) {
        StringBuilder sb = new StringBuilder();
        sb.append("Olá, ").append(reservation.getGuestName()).append("!\n\n");
        sb.append("Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n");
        sb.append(generateReceipt(reservation, roomType));
        sb.append("\nA entrada é a partir das 14h, e a saída, até as 12h.\n");
        sb.append("Até breve!\n");

        return sb.toString();
    }

    private static String formatCurrency(BigDecimal value) {
        return String.format("R$ %.2f", value);
    }
}
