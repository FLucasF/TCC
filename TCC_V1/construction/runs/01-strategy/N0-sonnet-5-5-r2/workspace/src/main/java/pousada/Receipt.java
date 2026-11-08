package pousada;

import java.time.format.DateTimeFormatter;

/** Comprovante de reserva em texto. */
public final class Receipt {
    private static final DateTimeFormatter BR_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private Receipt() {}

    public static String text(Reservation r) {
        StringBuilder sb = new StringBuilder();
        sb.append("Comprovante de reserva nº ").append(r.id()).append('\n');
        sb.append("Hóspede: ").append(r.guestName()).append(" (CPF ").append(formatCpf(r.guestCpf())).append(")\n");
        sb.append("Quarto: ").append(r.roomNumber()).append(" (").append(r.roomType()).append(")\n");
        sb.append("Entrada: ").append(r.checkIn().format(BR_DATE))
          .append(" | Saída: ").append(r.checkOut().format(BR_DATE))
          .append(" | ").append(r.nights()).append(r.nights() == 1 ? " diária" : " diárias").append('\n');
        sb.append("Diárias: ").append(Money.brl(r.subtotal())).append('\n');
        if (r.discount().signum() > 0) {
            sb.append("Desconto: ").append(Money.brl(r.discount())).append('\n');
        }
        sb.append("Total: ").append(Money.brl(r.total()));
        return sb.toString();
    }

    public static String confirmationEmailBody(Reservation r) {
        return "Olá, " + r.guestName() + "!\n\n"
                + "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                + text(r) + "\n\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                + "Até breve!";
    }

    /** 11 dígitos viram 000.000.000-00; qualquer outra coisa é mostrada como veio. */
    static String formatCpf(String cpf) {
        String d = cpf.replaceAll("\\D", "");
        if (d.length() != 11) return cpf;
        return d.substring(0, 3) + "." + d.substring(3, 6) + "." + d.substring(6, 9) + "-" + d.substring(9);
    }
}
