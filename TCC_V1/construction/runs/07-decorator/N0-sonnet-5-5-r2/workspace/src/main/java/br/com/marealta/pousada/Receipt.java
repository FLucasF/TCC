package br.com.marealta.pousada;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Comprovante de reserva: dados e texto. */
public record Receipt(long reservationId, String guestName, String guestCpf, int roomNumber, String roomType,
                      LocalDate checkIn, LocalDate checkOut, int nights, BigDecimal subtotal,
                      BigDecimal discount, BigDecimal total) {

    public static final String TITLE = "Comprovante de reserva";
    private static final DateTimeFormatter BR_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static String brl(BigDecimal v) {
        DecimalFormatSymbols s = new DecimalFormatSymbols(Locale.ROOT);
        s.setDecimalSeparator(',');
        s.setGroupingSeparator('.');
        return "R$ " + new DecimalFormat("#,##0.00", s).format(v);
    }

    private static String cpf(String raw) {
        if (raw.matches("\\d{11}")) {
            return raw.substring(0, 3) + "." + raw.substring(3, 6) + "." + raw.substring(6, 9) + "-" + raw.substring(9);
        }
        return raw;
    }

    public String text() {
        StringBuilder sb = new StringBuilder();
        sb.append(TITLE).append(" nº ").append(reservationId).append('\n');
        sb.append("Hóspede: ").append(guestName).append(" (CPF ").append(cpf(guestCpf)).append(")\n");
        sb.append("Quarto: ").append(roomNumber).append(" (").append(roomType).append(")\n");
        sb.append("Entrada: ").append(BR_DATE.format(checkIn)).append(" | Saída: ").append(BR_DATE.format(checkOut))
                .append(" | ").append(nights).append(nights == 1 ? " diária" : " diárias").append('\n');
        sb.append("Diárias: ").append(brl(subtotal)).append('\n');
        if (discount.signum() > 0) {
            sb.append("Desconto: ").append(brl(discount)).append('\n');
        }
        sb.append("Total: ").append(brl(total));
        return sb.toString();
    }

    public String confirmationEmailBody() {
        return "Olá, " + guestName + "!\n\n"
                + "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                + text() + "\n\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                + "Até breve!";
    }

    public static final String CONFIRMATION_SUBJECT = "Reserva confirmada - Pousada Maré Alta";
}
