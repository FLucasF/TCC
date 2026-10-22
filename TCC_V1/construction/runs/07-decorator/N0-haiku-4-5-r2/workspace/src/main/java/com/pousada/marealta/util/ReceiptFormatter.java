package com.pousada.marealta.util;

import com.pousada.marealta.models.Receipt;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class ReceiptFormatter {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter CURRENCY_FORMATTER = DateTimeFormatter.ofPattern("0.00");

    public static String format(Receipt receipt) {
        StringBuilder sb = new StringBuilder();
        sb.append("Comprovante de reserva nº ").append(receipt.reservationId).append("\n");
        sb.append("Hóspede: ").append(receipt.guestName).append(" (CPF ").append(receipt.guestCpf).append(")\n");
        sb.append("Quarto: ").append(receipt.roomNumber).append(" (").append(receipt.roomType).append(")\n");
        sb.append("Entrada: ").append(formatDate(receipt.checkIn));
        sb.append(" | Saída: ").append(formatDate(receipt.checkOut));
        sb.append(" | ").append(receipt.nights).append(receipt.nights == 1 ? " diária" : " diárias").append("\n");
        sb.append("Diárias: ").append(formatCurrency(receipt.subtotal)).append("\n");

        if (receipt.discount.compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Desconto: ").append(formatCurrency(receipt.discount)).append("\n");
        }

        sb.append("Total: ").append(formatCurrency(receipt.total)).append("\n");

        return sb.toString().trim();
    }

    private static String formatDate(LocalDate date) {
        return date.format(DATE_FORMATTER);
    }

    private static String formatCurrency(BigDecimal value) {
        return "R$ " + value.toPlainString();
    }
}
