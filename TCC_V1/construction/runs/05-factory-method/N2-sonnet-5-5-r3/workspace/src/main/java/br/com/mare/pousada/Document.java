package br.com.mare.pousada;

import java.util.ArrayList;
import java.util.List;

/**
 * Documento da reserva. A sequência de linhas é fixa; cada tipo de documento
 * define só o título, as linhas do hóspede, o estilo e o que vem depois do total.
 */
public abstract class Document {
    protected final Reservation reservation;

    protected Document(Reservation reservation) {
        this.reservation = reservation;
    }

    public Reservation reservation() {
        return reservation;
    }

    public abstract String title();

    protected abstract DocumentStyle style();

    protected abstract List<String> guestLines();

    protected List<String> closingLines() {
        return List.of();
    }

    public final String text() {
        DocumentStyle s = style();
        Quote q = reservation.quote();
        List<String> lines = new ArrayList<>();
        lines.add(title() + " " + s.numberSign() + " " + reservation.id());
        lines.addAll(guestLines());
        lines.add(s.roomLabel() + ": " + reservation.room().number() + " (" + reservation.room().type() + ")");
        lines.add(s.checkInLabel() + ": " + s.date(reservation.checkIn())
                + " | " + s.checkOutLabel() + ": " + s.date(reservation.checkOut())
                + " | " + s.nights(q.nights()));
        lines.add(s.subtotalLabel() + ": " + s.money(q.subtotal()));
        if (q.discount().signum() > 0) {
            lines.add(s.discountLabel() + ": " + s.money(q.discount()));
        }
        lines.add(s.totalLabel() + ": " + s.money(q.total()));
        lines.addAll(closingLines());
        return String.join("\n", lines);
    }
}
