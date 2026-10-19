package br.com.marealta;

import java.time.format.DateTimeFormatter;

/** Comprovante de reserva em texto, usado no e-mail de confirmação. */
public final class Receipt {
    private Receipt() {}

    private static final DateTimeFormatter BR_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static String text(Reservation r) {
        StringBuilder sb = new StringBuilder();
        sb.append("Comprovante de reserva nº ").append(r.id).append('\n');
        sb.append("Hóspede: ").append(r.guestName).append(" (CPF ").append(r.guestCpf).append(")\n");
        sb.append("Quarto: ").append(r.room.number()).append(" (").append(r.room.type()).append(")\n");
        sb.append("Entrada: ").append(r.checkIn.format(BR_DATE))
                .append(" | Saída: ").append(r.checkOut.format(BR_DATE))
                .append(" | ").append(r.nights).append(r.nights == 1 ? " diária" : " diárias").append('\n');
        sb.append("Diárias: ").append(Money.brl(r.subtotal)).append('\n');
        if (r.discount.signum() > 0) {
            sb.append("Desconto: ").append(Money.brl(r.discount)).append('\n');
        }
        sb.append("Total: ").append(Money.brl(r.total));
        return sb.toString();
    }

    public static String confirmationSubject() {
        return "Reserva confirmada - Pousada Maré Alta";
    }

    public static String confirmationBody(Reservation r) {
        return "Olá, " + r.guestName + "!\n\n"
                + "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                + text(r) + "\n\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                + "Até breve!";
    }
}
