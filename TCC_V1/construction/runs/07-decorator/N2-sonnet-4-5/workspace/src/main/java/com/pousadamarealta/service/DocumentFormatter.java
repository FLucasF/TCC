package com.pousadamarealta.service;

import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.Room;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class DocumentFormatter {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Locale BRAZILIAN_LOCALE = new Locale("pt", "BR");

    public static String formatReceipt(Reservation reservation, Room room) {
        StringBuilder sb = new StringBuilder();

        sb.append("Comprovante de reserva nº ").append(reservation.id()).append("\n");
        sb.append("Hóspede: ").append(reservation.guestName())
            .append(" (CPF ").append(formatCpf(reservation.guestCpf())).append(")\n");
        sb.append("Quarto: ").append(room.number()).append(" (").append(room.type().name()).append(")\n");
        sb.append("Entrada: ").append(formatDate(reservation.checkIn()))
            .append(" | Saída: ").append(formatDate(reservation.checkOut()))
            .append(" | ").append(formatNights(reservation.nights())).append("\n");
        sb.append("Diárias: ").append(formatMoney(reservation.subtotal())).append("\n");

        if (reservation.discount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Desconto: ").append(formatMoney(reservation.discount())).append("\n");
        }

        sb.append("Total: ").append(formatMoney(reservation.total()));

        return sb.toString();
    }

    private static String formatDate(LocalDate date) {
        return date.format(DATE_FORMATTER);
    }

    private static String formatMoney(BigDecimal amount) {
        return String.format(BRAZILIAN_LOCALE, "R$ %,.2f", amount);
    }

    private static String formatNights(int nights) {
        return nights == 1 ? "1 diária" : nights + " diárias";
    }

    private static String formatCpf(String cpf) {
        if (cpf.length() == 11) {
            return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." + cpf.substring(6, 9) + "-" + cpf.substring(9);
        }
        return cpf;
    }
}
