package br.com.marealta;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public final class Receipt {
    public static final String TITLE = "Comprovante de reserva";
    private static final DateTimeFormatter BR_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private Receipt() {}

    public static String date(LocalDate d) {
        return d.format(BR_DATE);
    }

    public static String text(Reservation r, Room room) {
        StringBuilder sb = new StringBuilder();
        sb.append(TITLE).append(" nº ").append(r.id()).append('\n');
        sb.append("Hóspede: ").append(r.guestName()).append(" (CPF ").append(r.guestCpf()).append(")\n");
        sb.append("Quarto: ").append(room.number()).append(" (").append(room.type()).append(")\n");
        sb.append("Entrada: ").append(date(r.checkIn())).append(" | Saída: ").append(date(r.checkOut()))
                .append(" | ").append(r.nights()).append(r.nights() == 1 ? " diária" : " diárias").append('\n');
        sb.append("Diárias: ").append(Money.brl(r.subtotal())).append('\n');
        if (r.discount().signum() > 0) {
            sb.append("Desconto: ").append(Money.brl(r.discount())).append('\n');
        }
        sb.append("Total: ").append(Money.brl(r.total()));
        return sb.toString();
    }

    public static String confirmationEmail(Reservation r, Room room) {
        return "Olá, " + r.guestName() + "!\n\n"
                + "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                + text(r, room) + "\n\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                + "Até breve!";
    }
}
