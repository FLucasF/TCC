package com.pousadamarealta.util;

import com.pousadamarealta.domain.Reservation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class FormatUtil {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Locale BRAZILIAN_LOCALE = new Locale("pt", "BR");

    public static String formatDate(LocalDate date) {
        return date.format(DATE_FORMATTER);
    }

    public static String formatMoney(BigDecimal amount) {
        return String.format(BRAZILIAN_LOCALE, "R$ %,.2f", amount);
    }

    public static String formatCpf(String cpf) {
        if (cpf.length() == 11) {
            return String.format("%s.%s.%s-%s",
                cpf.substring(0, 3),
                cpf.substring(3, 6),
                cpf.substring(6, 9),
                cpf.substring(9, 11)
            );
        }
        return cpf;
    }

    public static String formatReceipt(Reservation reservation) {
        StringBuilder receipt = new StringBuilder();
        receipt.append(String.format("Comprovante de reserva nº %d\n", reservation.getId()));
        receipt.append(String.format("Hóspede: %s (CPF %s)\n",
            reservation.getGuestName(),
            formatCpf(reservation.getGuestCpf())));
        receipt.append(String.format("Quarto: %s (%s)\n",
            reservation.getRoomNumber(),
            getRoomType(reservation.getRoomNumber())));
        receipt.append(String.format("Entrada: %s | Saída: %s | %d %s\n",
            formatDate(reservation.getCheckIn()),
            formatDate(reservation.getCheckOut()),
            reservation.getNights(),
            reservation.getNights() == 1 ? "diária" : "diárias"));
        receipt.append(String.format("Diárias: %s\n", formatMoney(reservation.getSubtotal())));

        if (reservation.getDiscount() != null && reservation.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            receipt.append(String.format("Desconto: %s\n", formatMoney(reservation.getDiscount())));
        }

        receipt.append(String.format("Total: %s", formatMoney(reservation.getTotal())));

        return receipt.toString();
    }

    private static String getRoomType(String roomNumber) {
        if (roomNumber.startsWith("1")) return "STANDARD";
        if (roomNumber.startsWith("2")) return "DELUXE";
        return "SUITE";
    }
}
