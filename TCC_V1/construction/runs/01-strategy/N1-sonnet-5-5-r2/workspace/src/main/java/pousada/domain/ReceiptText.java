package pousada.domain;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Texto do comprovante e do e-mail de confirmação. */
public final class ReceiptText {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private ReceiptText() {
    }

    public static String money(BigDecimal value) {
        DecimalFormat format = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.forLanguageTag("pt-BR")));
        return "R$ " + format.format(value);
    }

    public static String date(LocalDate date) {
        return DATE.format(date);
    }

    public static String receipt(Receipt r) {
        List<String> lines = new ArrayList<>();
        lines.add(r.title() + " nº " + r.reservationId());
        lines.add("Hóspede: " + r.guestName() + " (CPF " + r.guestCpf() + ")");
        lines.add("Quarto: " + r.roomNumber() + " (" + r.roomType() + ")");
        lines.add("Entrada: " + date(r.checkIn()) + " | Saída: " + date(r.checkOut()) + " | "
                + r.nights() + (r.nights() == 1 ? " diária" : " diárias"));
        lines.add("Diárias: " + money(r.subtotal()));
        if (r.discount().signum() > 0) {
            lines.add("Desconto: " + money(r.discount()));
        }
        lines.add("Total: " + money(r.total()));
        return String.join("\n", lines);
    }

    public static final String CONFIRMATION_SUBJECT = "Reserva confirmada - Pousada Maré Alta";

    public static String confirmationEmail(Receipt r) {
        return "Olá, " + r.guestName() + "!\n\n"
                + "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                + receipt(r) + "\n\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                + "Até breve!";
    }
}
