package br.com.marealta;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Sequência fixa de um documento da reserva; cada formato define só o que é dele
 * (títulos, idioma, linha do hóspede, linhas finais).
 */
public abstract class DocumentFormat {
    public final String render(Reservation r) {
        List<String> lines = new ArrayList<>();
        lines.add(title() + " " + numberMarker() + " " + r.id());
        lines.addAll(partyLines(r));
        lines.add(roomLabel() + ": " + r.room().number() + " (" + r.room().type() + ")");
        lines.add(checkInLabel() + ": " + date(r.checkIn()) + " | " + checkOutLabel() + ": " + date(r.checkOut())
                + " | " + nightsText(r.price().nights()));
        lines.add(subtotalLabel() + ": " + money(r.price().subtotal()));
        if (r.price().discount().signum() > 0) {
            lines.add(discountLabel() + ": " + money(r.price().discount()));
        }
        lines.add(totalLabel() + ": " + money(r.price().total()));
        lines.addAll(closingLines(r));
        return String.join("\n", lines);
    }

    public abstract String title();

    protected abstract String numberMarker();

    protected abstract List<String> partyLines(Reservation r);

    protected abstract String roomLabel();

    protected abstract String checkInLabel();

    protected abstract String checkOutLabel();

    protected abstract String nightsText(int nights);

    protected abstract String subtotalLabel();

    protected abstract String discountLabel();

    protected abstract String totalLabel();

    protected abstract String money(BigDecimal value);

    protected abstract String date(LocalDate date);

    protected List<String> closingLines(Reservation r) { return List.of(); }
}
