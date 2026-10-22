package com.pousada.mare_alta.document;

import com.pousada.mare_alta.model.Document;
import com.pousada.mare_alta.util.NumberFormatter;
import java.time.format.DateTimeFormatter;

public class CpfDocumentFormatter implements DocumentFormatter {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public Document format(DocumentData data) {
        String nightsText = data.getNights() == 1 ? "1 diária" : data.getNights() + " diárias";

        StringBuilder text = new StringBuilder();
        text.append("Comprovante de reserva nº ").append(data.getReservationId()).append("\n");
        text.append("Hóspede: ").append(data.getGuestName()).append(" (CPF ").append(formatCpf(data.getGuestCpf())).append(")\n");
        text.append("Quarto: ").append(data.getRoomNumber()).append(" (").append(data.getRoomType()).append(")\n");
        text.append("Entrada: ").append(data.getCheckIn().format(DATE_FORMATTER));
        text.append(" | Saída: ").append(data.getCheckOut().format(DATE_FORMATTER));
        text.append(" | ").append(nightsText).append("\n");
        text.append("Diárias: ").append(NumberFormatter.formatCurrency(data.getSubtotal())).append("\n");

        if (data.getDiscount().signum() > 0) {
            text.append("Desconto: ").append(NumberFormatter.formatCurrency(data.getDiscount())).append("\n");
        }

        text.append("Total: ").append(NumberFormatter.formatCurrency(data.getTotal())).append("\n");

        return new Document(
                "Comprovante de reserva",
                data.getReservationId(), data.getGuestName(),
                data.getGuestCpf(), null, null, null, null,
                data.getRoomNumber(), data.getRoomType(),
                data.getCheckIn(), data.getCheckOut(), data.getNights(),
                data.getSubtotal(), data.getDiscount(), data.getTotal(),
                text.toString().trim());
    }

    private String formatCpf(String cpf) {
        if (cpf == null || cpf.length() != 11) {
            return cpf;
        }
        return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." + cpf.substring(6, 9) + "-" + cpf.substring(9);
    }
}
