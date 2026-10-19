package com.pousada.marealta.util;

import com.pousada.marealta.model.ReservationDocument;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

public class DocumentFormatter {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static String formatDocument(ReservationDocument doc) {
        StringBuilder sb = new StringBuilder();
        sb.append("Comprovante de reserva nº ").append(doc.reservationId()).append("\n");
        sb.append("Hóspede: ").append(doc.guestName());
        sb.append(" (CPF ").append(formatCpf(doc.guestCpf())).append(")\n");
        sb.append("Quarto: ").append(doc.roomNumber()).append(" (").append(doc.roomType()).append(")\n");
        sb.append("Entrada: ").append(doc.checkIn().format(DATE_FORMAT));
        sb.append(" | Saída: ").append(doc.checkOut().format(DATE_FORMAT));
        String nightsLabel = doc.nights() == 1 ? "1 diária" : doc.nights() + " diárias";
        sb.append(" | ").append(nightsLabel).append("\n");
        sb.append("Diárias: ").append(formatMoney(doc.subtotal())).append("\n");

        if (doc.discount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Desconto: ").append(formatMoney(doc.discount())).append("\n");
        }

        sb.append("Total: ").append(formatMoney(doc.total())).append("\n");

        return sb.toString();
    }

    public static String formatMoney(BigDecimal amount) {
        return "R$ " + String.format("%.2f", amount).replace('.', ',');
    }

    public static String formatCpf(String cpf) {
        if (cpf.length() == 11) {
            return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." +
                   cpf.substring(6, 9) + "-" + cpf.substring(9);
        }
        return cpf;
    }
}
