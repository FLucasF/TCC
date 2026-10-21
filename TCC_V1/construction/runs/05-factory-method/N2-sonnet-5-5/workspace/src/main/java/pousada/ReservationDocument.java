package pousada;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Documento emitido para a reserva. A sequência do texto é a mesma para todos; cada tipo de
 * documento muda só o título, o idioma, as linhas do hóspede e o fechamento.
 */
public abstract class ReservationDocument {
    protected final DocumentData data;
    private final GuestIdentity identity;

    protected ReservationDocument(DocumentData data, GuestIdentity identity) {
        this.data = data;
        this.identity = identity;
    }

    protected abstract String title();

    protected abstract DocumentLabels labels();

    protected abstract List<String> guestLines();

    protected List<String> closingLines() { return List.of(); }

    public final String text() {
        DocumentLabels l = labels();
        Quote quote = data.quote();
        List<String> lines = new ArrayList<>();
        lines.add(title() + " " + l.number() + " " + data.reservationId());
        lines.addAll(guestLines());
        lines.add(l.room() + ": " + data.roomNumber() + " (" + data.roomType() + ")");
        lines.add(l.checkIn() + ": " + l.date().format(data.checkIn())
                + " | " + l.checkOut() + ": " + l.date().format(data.checkOut())
                + " | " + quote.nights() + " " + (quote.nights() == 1 ? l.nightOne() : l.nightMany()));
        lines.add(l.subtotal() + ": " + money(quote.subtotal()));
        if (quote.discount().signum() > 0) {
            lines.add(l.discount() + ": " + money(quote.discount()));
        }
        lines.add(l.total() + ": " + money(quote.total()));
        lines.addAll(closingLines());
        return String.join("\n", lines);
    }

    protected final String money(BigDecimal value) {
        return labels().money().apply(value);
    }

    public final String titleText() { return title(); }

    public final DocumentData data() { return data; }

    public final GuestIdentity identity() { return identity; }
}
