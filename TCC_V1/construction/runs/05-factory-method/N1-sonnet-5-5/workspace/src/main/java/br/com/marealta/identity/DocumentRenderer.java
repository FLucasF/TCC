package br.com.marealta.identity;

import br.com.marealta.domain.Quote;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** A sequência de linhas é a mesma para todo documento; cada tipo de identificação muda só o que é dele. */
public final class DocumentRenderer {
    private DocumentRenderer() {}

    public static String render(DocumentData data) {
        IdentityKind kind = data.identity().kind();
        Language lang = kind.language();
        Quote quote = data.quote();

        List<String> lines = new ArrayList<>();
        lines.add(kind.documentTitle() + " " + lang.numberLabel() + " " + data.reservationId());
        lines.addAll(kind.identityLines(data.guestName(), data.identity().values()));
        lines.add(lang.room() + ": " + data.roomNumber() + " (" + data.roomType() + ")");
        lines.add(lang.checkIn() + ": " + lang.date(data.checkIn()) + " | "
                + lang.checkOut() + ": " + lang.date(data.checkOut()) + " | " + lang.nights(quote.nights()));
        lines.add(lang.subtotal() + ": " + lang.money(quote.subtotal()));
        if (quote.discount().compareTo(BigDecimal.ZERO) > 0) {
            lines.add(lang.discount() + ": " + lang.money(quote.discount()));
        }
        lines.add(lang.total() + ": " + lang.money(quote.total()));
        lines.addAll(kind.closingLines(quote.total()));
        return String.join("\n", lines);
    }
}
