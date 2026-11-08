package pousada.domain;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** O comprovante escrito, igual no e-mail e na segunda via. */
public final class ReceiptText {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private ReceiptText() {
    }

    public static String of(Receipt r) {
        List<String> lines = new ArrayList<>();
        lines.add(r.title() + " nº " + r.reservationId());
        lines.add("Hóspede: " + r.guestName() + " (CPF " + r.guestCpf() + ")");
        lines.add("Quarto: " + r.roomNumber() + " (" + r.roomType() + ")");
        lines.add("Entrada: " + date(r.checkIn()) + " | Saída: " + date(r.checkOut()) + " | "
                + r.nights() + (r.nights() == 1 ? " diária" : " diárias"));
        lines.add("Diárias: " + Money.brl(r.subtotal()));
        if (r.discount().signum() > 0) {
            lines.add("Desconto: " + Money.brl(r.discount()));
        }
        lines.add("Total: " + Money.brl(r.total()));
        return String.join("\n", lines);
    }

    private static String date(LocalDate date) {
        return DATE.format(date);
    }
}
