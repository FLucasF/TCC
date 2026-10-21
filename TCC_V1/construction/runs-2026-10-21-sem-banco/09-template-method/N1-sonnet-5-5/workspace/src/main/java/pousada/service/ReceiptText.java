package pousada.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import pousada.domain.Receipt;

public final class ReceiptText {
    private ReceiptText() {
    }

    public static String render(Receipt r) {
        List<String> lines = new ArrayList<>();
        lines.add(r.title() + " nº " + r.reservationId());
        lines.add("Hóspede: " + r.guestName() + " (CPF " + r.guestCpf() + ")");
        lines.add("Quarto: " + r.roomNumber() + " (" + r.roomType() + ")");
        lines.add("Entrada: " + Formats.brDate(r.checkIn()) + " | Saída: " + Formats.brDate(r.checkOut())
                + " | " + r.nights() + (r.nights() == 1 ? " diária" : " diárias"));
        lines.add("Diárias: " + Formats.brMoney(r.subtotal()));
        if (r.discount().compareTo(BigDecimal.ZERO) > 0) {
            lines.add("Desconto: " + Formats.brMoney(r.discount()));
        }
        lines.add("Total: " + Formats.brMoney(r.total()));
        return String.join("\n", lines);
    }
}
