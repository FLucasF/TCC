package pousada.document;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Idioma e formatos de um documento, e a sequência de linhas que todos eles seguem.
 * Cada tipo de documento só informa o título, as linhas de identificação e o rodapé.
 */
public record Wording(
        String numberSign,
        String roomLabel,
        String checkInLabel,
        String checkOutLabel,
        String oneNight,
        String manyNights,
        String subtotalLabel,
        String discountLabel,
        String totalLabel,
        String moneyPrefix,
        char groupingSeparator,
        char decimalSeparator,
        DateTimeFormatter dateFormat) {

    public static final Wording PORTUGUESE = new Wording("nº", "Quarto", "Entrada", "Saída",
            "diária", "diárias", "Diárias", "Desconto", "Total",
            "R$ ", '.', ',', DateTimeFormatter.ofPattern("dd/MM/yyyy"));

    public static final Wording ENGLISH = new Wording("no.", "Room", "Check-in", "Check-out",
            "night", "nights", "Nights", "Discount", "Total",
            "BRL ", ',', '.', DateTimeFormatter.ISO_LOCAL_DATE);

    public String money(BigDecimal value) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator(groupingSeparator);
        symbols.setDecimalSeparator(decimalSeparator);
        return moneyPrefix + new DecimalFormat("#,##0.00", symbols).format(value);
    }

    public String date(LocalDate date) {
        return dateFormat.format(date);
    }

    public String render(String title, Stay stay, List<String> identityLines, List<String> footerLines) {
        List<String> lines = new ArrayList<>();
        lines.add(title + " " + numberSign + " " + stay.reservationId());
        lines.addAll(identityLines);
        lines.add(roomLabel + ": " + stay.roomNumber() + " (" + stay.roomType() + ")");
        lines.add(checkInLabel + ": " + date(stay.checkIn()) + " | " + checkOutLabel + ": " + date(stay.checkOut())
                + " | " + stay.nights() + " " + (stay.nights() == 1 ? oneNight : manyNights));
        lines.add(subtotalLabel + ": " + money(stay.subtotal()));
        if (stay.discount().signum() > 0) {
            lines.add(discountLabel + ": " + money(stay.discount()));
        }
        lines.add(totalLabel + ": " + money(stay.total()));
        lines.addAll(footerLines);
        return String.join("\n", lines);
    }
}
