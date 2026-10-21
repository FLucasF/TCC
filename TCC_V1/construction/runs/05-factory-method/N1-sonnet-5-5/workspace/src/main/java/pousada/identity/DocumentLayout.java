package pousada.identity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import pousada.Money;
import pousada.Quote;

/** A sequência de linhas comum a todos os documentos; cada documento muda só o que é dele. */
final class DocumentLayout {

    record Labels(String number, String room, String checkIn, String checkOut,
                  String nightSingular, String nightPlural,
                  String subtotal, String discount, String total,
                  Function<BigDecimal, String> money, Function<LocalDate, String> date) {
    }

    static final Labels PORTUGUESE = new Labels("nº", "Quarto", "Entrada", "Saída",
            "diária", "diárias", "Diárias", "Desconto", "Total",
            v -> "R$ " + Money.brazilian(v),
            d -> d.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

    static final Labels ENGLISH = new Labels("no.", "Room", "Check-in", "Check-out",
            "night", "nights", "Nights", "Discount", "Total",
            v -> "BRL " + Money.english(v),
            LocalDate::toString);

    private DocumentLayout() {
    }

    static String render(Labels l, String title, DocumentData data, List<String> partyLines, List<String> footerLines) {
        Quote quote = data.quote();
        List<String> lines = new ArrayList<>();
        lines.add(title + " " + l.number() + " " + data.reservationId());
        lines.addAll(partyLines);
        lines.add(l.room() + ": " + data.room().number() + " (" + data.room().type() + ")");
        lines.add(l.checkIn() + ": " + l.date().apply(data.checkIn())
                + " | " + l.checkOut() + ": " + l.date().apply(data.checkOut())
                + " | " + quote.nights() + " " + (quote.nights() == 1 ? l.nightSingular() : l.nightPlural()));
        lines.add(l.subtotal() + ": " + l.money().apply(quote.subtotal()));
        if (quote.discount().signum() > 0) {
            lines.add(l.discount() + ": " + l.money().apply(quote.discount()));
        }
        lines.add(l.total() + ": " + l.money().apply(quote.total()));
        lines.addAll(footerLines);
        return String.join("\n", lines);
    }
}
