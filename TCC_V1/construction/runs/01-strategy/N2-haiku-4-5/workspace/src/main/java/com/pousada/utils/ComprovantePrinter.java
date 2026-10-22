package com.pousada.utils;

import com.pousada.domain.Reservation;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class ComprovantePrinter {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static String generate(Reservation reservation, String roomType) {
        StringBuilder sb = new StringBuilder();

        sb.append("Comprovante de reserva nº ").append(reservation.getId()).append("\n");
        sb.append("Hóspede: ").append(reservation.getGuestName())
            .append(" (CPF ").append(formatCpf(reservation.getGuestCpf())).append(")\n");
        sb.append("Quarto: ").append(reservation.getRoomNumber())
            .append(" (").append(roomType).append(")\n");

        sb.append("Entrada: ").append(reservation.getCheckIn().format(DATE_FORMAT));
        sb.append(" | Saída: ").append(reservation.getCheckOut().format(DATE_FORMAT));
        sb.append(" | ").append(formatNights(reservation.getNights())).append("\n");

        sb.append("Diárias: ").append(formatMoney(reservation.getSubtotal())).append("\n");

        if (reservation.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Desconto: ").append(formatMoney(reservation.getDiscount())).append("\n");
        }

        sb.append("Total: ").append(formatMoney(reservation.getTotal()));

        return sb.toString();
    }

    public static String formatMoney(BigDecimal value) {
        return String.format("R$ %.2f", value);
    }

    public static String formatCpf(String cpf) {
        if (cpf.length() != 11) return cpf;
        return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." +
               cpf.substring(6, 9) + "-" + cpf.substring(9);
    }

    private static String formatNights(int nights) {
        if (nights == 1) return "1 diária";
        return nights + " diárias";
    }
}
