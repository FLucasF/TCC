package br.com.maretalta;

import br.com.maretalta.domain.Money;
import br.com.maretalta.domain.Reservation;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

public final class ReceiptFormatter {
    public static final String TITLE = "Comprovante de reserva";
    private static final DateTimeFormatter BR_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private ReceiptFormatter() {}

    public static String text(Reservation r) {
        StringBuilder sb = new StringBuilder();
        sb.append(TITLE).append(" nº ").append(r.id).append('\n');
        sb.append("Hóspede: ").append(r.guestName).append(" (CPF ").append(formatCpf(r.guestCpf)).append(")\n");
        sb.append("Quarto: ").append(r.room.number()).append(" (").append(r.room.type()).append(")\n");
        sb.append("Entrada: ").append(r.checkIn.format(BR_DATE))
                .append(" | Saída: ").append(r.checkOut.format(BR_DATE))
                .append(" | ").append(r.nights).append(r.nights == 1 ? " diária" : " diárias").append('\n');
        sb.append("Diárias: ").append(Money.brl(r.subtotal)).append('\n');
        if (r.discount.compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Desconto: ").append(Money.brl(r.discount)).append('\n');
        }
        sb.append("Total: ").append(Money.brl(r.total));
        return sb.toString();
    }

    public static String confirmationEmail(Reservation r) {
        return "Olá, " + r.guestName + "!\n\n"
                + "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                + text(r) + "\n\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                + "Até breve!";
    }

    private static String formatCpf(String cpf) {
        if (cpf.matches("\\d{11}")) {
            return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." + cpf.substring(6, 9) + "-" + cpf.substring(9);
        }
        return cpf;
    }
}
