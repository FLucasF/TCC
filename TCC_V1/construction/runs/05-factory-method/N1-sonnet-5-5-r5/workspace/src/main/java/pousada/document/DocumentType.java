package pousada.document;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Um tipo de documento da reserva. A sequência de linhas é escrita aqui, uma vez só;
 * cada tipo informa os campos de identificação, o título, o idioma e o que muda no texto.
 * Para um tipo novo: criar a subclasse e acrescentá-la em {@link DocumentTypes}.
 */
public abstract class DocumentType {
    /** Código guardado no banco. */
    public abstract String code();

    /** Campos do corpo da reserva que identificam o hóspede neste tipo; todos são obrigatórios. */
    public abstract List<String> fields();

    public abstract String title();

    protected abstract Language language();

    /** Linhas de identificação, entre o título e o quarto. */
    protected abstract List<String> identityLines(String guestName, Map<String, String> identity);

    /** Linhas depois do total. */
    protected List<String> closingLines(DocumentData data) {
        return List.of();
    }

    public final String render(DocumentData data, Map<String, String> identity) {
        Language lang = language();
        List<String> lines = new ArrayList<>();
        lines.add(title() + " " + lang.number + " " + data.reservationId());
        lines.addAll(identityLines(data.guestName(), identity));
        lines.add(lang.room + ": " + data.roomNumber() + " (" + data.roomType() + ")");
        lines.add(lang.checkIn + ": " + lang.date(data.checkIn()) + " | "
                + lang.checkOut + ": " + lang.date(data.checkOut()) + " | " + lang.nights(data.nights()));
        lines.add(lang.subtotal + ": " + lang.money(data.subtotal()));
        if (data.discount().compareTo(BigDecimal.ZERO) > 0) {
            lines.add(lang.discount + ": " + lang.money(data.discount()));
        }
        lines.add(lang.total + ": " + lang.money(data.total()));
        lines.addAll(closingLines(data));
        return String.join("\n", lines);
    }
}
