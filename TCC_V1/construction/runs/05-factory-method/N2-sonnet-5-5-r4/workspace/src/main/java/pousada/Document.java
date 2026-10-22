package pousada;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Documento emitido para uma reserva paga. A sequência é fixa; cada tipo muda só as suas linhas. */
abstract class Document {

    abstract String title();

    final String render(Reservation r) {
        List<String> lines = new ArrayList<>();
        lines.add(titleLine(r));
        lines.addAll(identityLines(r));
        lines.add(roomLine(r));
        lines.add(periodLine(r));
        lines.add(subtotalLine(r));
        if (r.pricing().discount().signum() > 0) {
            lines.add(discountLine(r));
        }
        lines.add(totalLine(r));
        lines.addAll(closingLines(r));
        return String.join("\n", lines);
    }

    protected abstract String titleLine(Reservation r);

    protected abstract List<String> identityLines(Reservation r);

    protected abstract String roomLine(Reservation r);

    protected abstract String periodLine(Reservation r);

    protected abstract String subtotalLine(Reservation r);

    protected abstract String discountLine(Reservation r);

    protected abstract String totalLine(Reservation r);

    protected List<String> closingLines(Reservation r) {
        return Collections.emptyList();
    }
}
