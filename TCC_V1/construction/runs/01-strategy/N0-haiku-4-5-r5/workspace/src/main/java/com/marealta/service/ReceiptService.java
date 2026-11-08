package com.marealta.service;

import com.marealta.domain.Reservation;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

public class ReceiptService {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter CURRENCY_FORMAT = DateTimeFormatter.ofPattern("0.00");

    public static String generateReceipt(Reservation res, String roomType) {
        StringBuilder receipt = new StringBuilder();

        receipt.append("Comprovante de reserva nº ").append(res.id).append("\n");
        receipt.append("Hóspede: ").append(res.guestName).append(" (CPF ").append(res.guestCpf).append(")\n");
        receipt.append("Quarto: ").append(res.roomNumber).append(" (").append(roomType).append(")\n");
        receipt.append("Entrada: ").append(res.checkIn.format(DATE_FORMAT))
                .append(" | Saída: ").append(res.checkOut.format(DATE_FORMAT))
                .append(" | ").append(res.nights).append(res.nights == 1 ? " diária" : " diárias").append("\n");
        receipt.append("Diárias: R$ ").append(formatCurrency(res.subtotal)).append("\n");

        if (res.discount.compareTo(BigDecimal.ZERO) > 0) {
            receipt.append("Desconto: R$ ").append(formatCurrency(res.discount)).append("\n");
        }

        receipt.append("Total: R$ ").append(formatCurrency(res.total)).append("\n");

        return receipt.toString();
    }

    public static String generateConfirmationEmail(Reservation res, String roomType) {
        StringBuilder email = new StringBuilder();

        email.append("Olá, ").append(res.guestName).append("!\n\n");
        email.append("Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n");
        email.append(generateReceipt(res, roomType)).append("\n");
        email.append("A entrada é a partir das 14h, e a saída, até as 12h.\n");
        email.append("Até breve!\n");

        return email.toString();
    }

    private static String formatCurrency(BigDecimal value) {
        return String.format("%.2f", value).replace(".", ",");
    }
}
