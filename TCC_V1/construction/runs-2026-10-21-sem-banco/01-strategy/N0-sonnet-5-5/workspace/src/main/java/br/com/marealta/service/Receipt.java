package br.com.marealta.service;

import br.com.marealta.domain.Money;
import br.com.marealta.domain.Reservation;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Comprovante de reserva em texto. */
final class Receipt {
    static final String TITLE = "Comprovante de reserva";
    private static final DateTimeFormatter BR_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private Receipt() {}

    static String text(Reservation r) {
        List<String> lines = new ArrayList<>();
        lines.add(TITLE + " nº " + r.id());
        lines.add("Hóspede: " + r.guestName() + " (CPF " + r.guestCpf() + ")");
        lines.add("Quarto: " + r.room().number() + " (" + r.room().type() + ")");
        lines.add("Entrada: " + r.checkIn().format(BR_DATE) + " | Saída: " + r.checkOut().format(BR_DATE)
                + " | " + r.nights() + (r.nights() == 1 ? " diária" : " diárias"));
        lines.add("Diárias: " + Money.brl(r.subtotal()));
        if (r.discount().compareTo(BigDecimal.ZERO) > 0) {
            lines.add("Desconto: " + Money.brl(r.discount()));
        }
        lines.add("Total: " + Money.brl(r.total()));
        return String.join("\n", lines);
    }

    static String confirmationEmailBody(Reservation r) {
        return "Olá, " + r.guestName() + "!\n\n"
                + "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                + text(r) + "\n\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                + "Até breve!";
    }
}
