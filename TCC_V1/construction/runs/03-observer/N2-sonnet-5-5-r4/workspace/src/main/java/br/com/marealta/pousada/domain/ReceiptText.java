package br.com.marealta.pousada.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public final class ReceiptText {

    private ReceiptText() {
    }

    public static String of(Receipt r) {
        List<String> lines = new ArrayList<>();
        lines.add(r.title() + " nº " + r.reservationId());
        lines.add("Hóspede: " + r.guestName() + " (CPF " + r.guestCpf() + ")");
        lines.add("Quarto: " + r.roomNumber() + " (" + r.roomType() + ")");
        lines.add("Entrada: " + Formats.date(r.checkIn()) + " | Saída: " + Formats.date(r.checkOut())
                + " | " + r.nights() + (r.nights() == 1 ? " diária" : " diárias"));
        lines.add("Diárias: " + Money.format(r.subtotal()));
        if (r.discount().compareTo(BigDecimal.ZERO) > 0) {
            lines.add("Desconto: " + Money.format(r.discount()));
        }
        lines.add("Total: " + Money.format(r.total()));
        return String.join("\n", lines);
    }
}
