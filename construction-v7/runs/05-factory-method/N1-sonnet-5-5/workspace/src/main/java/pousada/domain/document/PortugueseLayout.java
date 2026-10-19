package pousada.domain.document;

import pousada.domain.Money;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Disposição em português: R$ 1.234,56 e 10/03/2027. */
public final class PortugueseLayout {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private PortugueseLayout() {
    }

    public static String render(String title, Voucher v, List<String> header, List<String> footer) {
        List<String> lines = new ArrayList<>();
        lines.add(title + " nº " + v.reservationId());
        lines.addAll(header);
        lines.add("Quarto: " + v.roomNumber() + " (" + v.roomType() + ")");
        lines.add("Entrada: " + DATE.format(v.checkIn()) + " | Saída: " + DATE.format(v.checkOut())
                + " | " + v.nights() + (v.nights() == 1 ? " diária" : " diárias"));
        lines.add("Diárias: " + Money.brl(v.subtotal()));
        if (v.hasDiscount()) {
            lines.add("Desconto: " + Money.brl(v.discount()));
        }
        lines.add("Total: " + Money.brl(v.total()));
        lines.addAll(footer);
        return String.join("\n", lines);
    }
}
