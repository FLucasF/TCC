package com.marealata.util;

import com.marealata.model.Reservation;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class DocumentFormatter {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final NumberFormat CURRENCY_FORMATTER = NumberFormat.getInstance(new Locale("pt", "BR"));

    static {
        CURRENCY_FORMATTER.setMinimumFractionDigits(2);
        CURRENCY_FORMATTER.setMaximumFractionDigits(2);
    }

    public static String formatReservationDocument(Reservation reservation, String roomType) {
        StringBuilder sb = new StringBuilder();
        sb.append("Comprovante de reserva nº ").append(reservation.id).append("\n");
        sb.append("Hóspede: ").append(reservation.guestName)
          .append(" (CPF ").append(formatCpf(reservation.guestCpf)).append(")\n");
        sb.append("Quarto: ").append(reservation.roomNumber).append(" (").append(roomType).append(")\n");
        sb.append("Entrada: ").append(formatDate(reservation.checkIn))
          .append(" | Saída: ").append(formatDate(reservation.checkOut))
          .append(" | ").append(reservation.nights).append(reservation.nights == 1 ? " diária" : " diárias").append("\n");
        sb.append("Diárias: ").append(formatCurrency(reservation.subtotal)).append("\n");

        if (reservation.discount.compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Desconto: ").append(formatCurrency(reservation.discount)).append("\n");
        }

        sb.append("Total: ").append(formatCurrency(reservation.total)).append("\n");

        return sb.toString().trim();
    }

    public static String formatDate(LocalDate date) {
        return date.format(DATE_FORMATTER);
    }

    public static String formatCurrency(BigDecimal amount) {
        return "R$ " + String.format("%.2f", amount).replace(".", ",");
    }

    public static String formatCurrencyCsv(BigDecimal amount) {
        return String.format("%.2f", amount);
    }

    public static String formatCpf(String cpf) {
        if (cpf == null || cpf.length() != 11) return cpf;
        return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." +
               cpf.substring(6, 9) + "-" + cpf.substring(9);
    }

    public static String formatDateCsv(LocalDate date) {
        return date.toString();
    }
}
