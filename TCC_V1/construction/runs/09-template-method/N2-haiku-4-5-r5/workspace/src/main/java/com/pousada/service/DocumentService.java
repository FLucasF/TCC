package com.pousada.service;

import com.pousada.domain.Reservation;
import com.pousada.domain.Room;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;

public class DocumentService {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DecimalFormat CURRENCY_FORMATTER = new DecimalFormat("0.00");

    public static String generateComprovante(Reservation reservation, Room room) {
        StringBuilder sb = new StringBuilder();
        sb.append("Comprovante de reserva nº ").append(reservation.id).append("\n");
        sb.append("Hóspede: ").append(reservation.guestName)
                .append(" (CPF ").append(formatCpf(reservation.guestCpf)).append(")\n");
        sb.append("Quarto: ").append(room.number).append(" (").append(room.type).append(")\n");
        sb.append("Entrada: ").append(reservation.checkIn.format(DATE_FORMATTER))
                .append(" | Saída: ").append(reservation.checkOut.format(DATE_FORMATTER))
                .append(" | ").append(reservation.nights).append(" ")
                .append(reservation.nights == 1 ? "diária" : "diárias").append("\n");
        sb.append("Diárias: ").append(formatCurrency(reservation.subtotal)).append("\n");

        if (reservation.discount.compareTo(java.math.BigDecimal.ZERO) > 0) {
            sb.append("Desconto: ").append(formatCurrency(reservation.discount)).append("\n");
        }

        sb.append("Total: ").append(formatCurrency(reservation.total));

        return sb.toString();
    }

    private static String formatCurrency(BigDecimal value) {
        return "R$ " + CURRENCY_FORMATTER.format(value).replace(".", ",");
    }

    private static String formatCpf(String cpf) {
        if (cpf.length() != 11) return cpf;
        return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." +
                cpf.substring(6, 9) + "-" + cpf.substring(9);
    }
}
