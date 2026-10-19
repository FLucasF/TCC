package br.com.mealta.pousada;

import java.util.ArrayList;
import java.util.List;

/** Trecho em português comum ao comprovante e à fatura. */
final class PtBrLayout {
    private PtBrLayout() {}

    static List<String> details(DocumentData d, boolean withGuestLine) {
        List<String> lines = new ArrayList<>();
        if (withGuestLine) {
            lines.add("Hóspede: " + d.guestName());
        }
        lines.add("Quarto: " + d.roomNumber() + " (" + d.roomType() + ")");
        lines.add("Entrada: " + Dates.br(d.checkIn()) + " | Saída: " + Dates.br(d.checkOut())
                + " | " + d.nights() + (d.nights() == 1 ? " diária" : " diárias"));
        lines.add("Diárias: " + Money.brl(d.subtotal()));
        if (d.hasDiscount()) {
            lines.add("Desconto: " + Money.brl(d.discount()));
        }
        lines.add("Total: " + Money.brl(d.total()));
        return lines;
    }
}
