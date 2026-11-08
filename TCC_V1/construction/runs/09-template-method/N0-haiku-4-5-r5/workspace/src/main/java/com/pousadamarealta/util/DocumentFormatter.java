package com.pousadamarealta.util;

import com.pousadamarealta.model.Document;
import java.math.BigDecimal;

public class DocumentFormatter {
    public static String formatAsText(Document doc) {
        StringBuilder sb = new StringBuilder();
        sb.append("Comprovante de reserva nº ").append(doc.reservationId).append("\n");
        sb.append(String.format("Hóspede: %s (CPF %s)\n", doc.guestName, formatCpf(doc.guestCpf)));
        sb.append(String.format("Quarto: %d (%s)\n", doc.roomNumber, doc.roomType));
        sb.append(String.format("Entrada: %s | Saída: %s | %d %s\n",
            DateFormatter.format(doc.checkIn),
            DateFormatter.format(doc.checkOut),
            doc.nights,
            doc.nights == 1 ? "diária" : "diárias"));
        sb.append(String.format("Diárias: %s\n", CurrencyFormatter.format(doc.subtotal)));

        if (doc.discount.compareTo(BigDecimal.ZERO) > 0) {
            sb.append(String.format("Desconto: %s\n", CurrencyFormatter.format(doc.discount)));
        }

        sb.append(String.format("Total: %s", CurrencyFormatter.format(doc.total)));
        return sb.toString();
    }

    private static String formatCpf(String cpf) {
        if (cpf == null || cpf.length() != 11) return cpf;
        return String.format("%s.%s.%s-%s",
            cpf.substring(0, 3),
            cpf.substring(3, 6),
            cpf.substring(6, 9),
            cpf.substring(9, 11));
    }
}
