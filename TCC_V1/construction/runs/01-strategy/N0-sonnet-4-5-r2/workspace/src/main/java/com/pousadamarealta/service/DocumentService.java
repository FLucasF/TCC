package com.pousadamarealta.service;

import com.pousadamarealta.domain.Reservation;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class DocumentService {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final NumberFormat CURRENCY_FORMATTER = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    public static String generateReceipt(Reservation reservation) {
        StringBuilder receipt = new StringBuilder();
        receipt.append(String.format("Comprovante de reserva nº %d%n", reservation.getId()));
        receipt.append(String.format("Hóspede: %s (CPF %s)%n",
            reservation.getGuestName(),
            formatCpf(reservation.getGuestCpf())));
        receipt.append(String.format("Quarto: %s (%s)%n",
            reservation.getRoomNumber(),
            reservation.getRatePlan() == com.pousadamarealta.domain.RatePlan.WEEKEND_PACKAGE ?
                getRoomTypeName(reservation.getRoomNumber()) :
                getRoomTypeName(reservation.getRoomNumber())));
        receipt.append(String.format("Entrada: %s | Saída: %s | %d %s%n",
            reservation.getCheckIn().format(DATE_FORMATTER),
            reservation.getCheckOut().format(DATE_FORMATTER),
            reservation.getNights(),
            reservation.getNights() == 1 ? "diária" : "diárias"));
        receipt.append(String.format("Diárias: %s%n", formatCurrency(reservation.getSubtotal())));

        if (reservation.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            receipt.append(String.format("Desconto: %s%n", formatCurrency(reservation.getDiscount())));
        }

        receipt.append(String.format("Total: %s", formatCurrency(reservation.getTotal())));

        return receipt.toString();
    }

    private static String getRoomTypeName(String roomNumber) {
        return switch (roomNumber) {
            case "101", "102" -> "STANDARD";
            case "201", "202" -> "DELUXE";
            case "301" -> "SUITE";
            default -> "UNKNOWN";
        };
    }

    private static String formatCpf(String cpf) {
        if (cpf.length() == 11) {
            return String.format("%s.%s.%s-%s",
                cpf.substring(0, 3),
                cpf.substring(3, 6),
                cpf.substring(6, 9),
                cpf.substring(9, 11));
        }
        return cpf;
    }

    private static String formatCurrency(BigDecimal amount) {
        return CURRENCY_FORMATTER.format(amount);
    }
}
