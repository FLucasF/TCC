package com.pousadamarealta.util;

import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.Room;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class DocumentFormatter {
    private static final Locale PT_BR = new Locale("pt", "BR");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final NumberFormat CURRENCY_FORMATTER = NumberFormat.getCurrencyInstance(PT_BR);

    public static String formatDocument(Reservation reservation) {
        StringBuilder doc = new StringBuilder();

        doc.append("Comprovante de reserva nº ").append(reservation.getId()).append("\n");
        doc.append("Hóspede: ").append(reservation.getGuestName())
           .append(" (CPF ").append(formatCpf(reservation.getGuestCpf())).append(")\n");
        doc.append("Quarto: ").append(reservation.getRoomNumber())
           .append(" (").append(reservation.getRatePlanType().name()).append(")\n");
        doc.append("Entrada: ").append(formatDate(reservation.getCheckIn()))
           .append(" | Saída: ").append(formatDate(reservation.getCheckOut()))
           .append(" | ").append(formatNights(reservation.getNights())).append("\n");
        doc.append("Diárias: ").append(formatCurrency(reservation.getSubtotal())).append("\n");

        if (reservation.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            doc.append("Desconto: ").append(formatCurrency(reservation.getDiscount())).append("\n");
        }

        doc.append("Total: ").append(formatCurrency(reservation.getTotal()));

        return doc.toString();
    }

    private static String formatCpf(String cpf) {
        if (cpf.length() == 11) {
            return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." +
                   cpf.substring(6, 9) + "-" + cpf.substring(9);
        }
        return cpf;
    }

    private static String formatDate(LocalDate date) {
        return date.format(DATE_FORMATTER);
    }

    private static String formatNights(long nights) {
        return nights == 1 ? "1 diária" : nights + " diárias";
    }

    private static String formatCurrency(BigDecimal value) {
        return CURRENCY_FORMATTER.format(value);
    }
}
