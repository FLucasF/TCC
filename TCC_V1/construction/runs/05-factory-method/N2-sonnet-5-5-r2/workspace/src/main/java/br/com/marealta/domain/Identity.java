package br.com.marealta.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Como o hóspede se identifica. Cada identificação emite o seu documento; a sequência de linhas é a mesma
 * e cada caso define apenas o que é dele.
 */
public abstract class Identity {

    /** Campos informados (nome do campo na API -> valor). */
    public abstract Map<String, String> fields();

    protected abstract String title();

    protected abstract String numberMark();

    protected abstract List<String> partyLines(ReceiptData data);

    protected abstract String roomLabel();

    protected abstract String periodLine(ReceiptData data);

    protected abstract String amountLabel();

    protected abstract String discountLabel();

    protected abstract String totalLabel();

    protected abstract String money(BigDecimal value);

    /** Linhas depois do total. */
    protected List<String> footerLines(ReceiptData data) {
        return List.of();
    }

    public final Document document(ReceiptData data) {
        List<String> lines = new ArrayList<>();
        lines.add(title() + " " + numberMark() + " " + data.reservationId());
        lines.addAll(partyLines(data));
        lines.add(roomLabel() + ": " + data.roomNumber() + " (" + data.roomType() + ")");
        lines.add(periodLine(data));
        lines.add(amountLabel() + ": " + money(data.subtotal()));
        if (data.discount().signum() > 0) {
            lines.add(discountLabel() + ": " + money(data.discount()));
        }
        lines.add(totalLabel() + ": " + money(data.total()));
        lines.addAll(footerLines(data));
        return new Document(title(), data, fields(), String.join("\n", lines));
    }
}
