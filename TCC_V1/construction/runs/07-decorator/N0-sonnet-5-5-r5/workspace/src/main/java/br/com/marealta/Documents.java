package br.com.marealta;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Textos do comprovante e do e-mail de confirmação. */
public final class Documents {
    private static final DateTimeFormatter BR_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private Documents() {}

    public static String receiptText(Receipt r) {
        List<String> lines = new ArrayList<>();
        lines.add(r.title() + " nº " + r.reservationId());
        lines.add("Hóspede: " + r.guestName() + " (CPF " + formatCpf(r.guestCpf()) + ")");
        lines.add("Quarto: " + r.roomNumber() + " (" + r.roomType() + ")");
        lines.add("Entrada: " + r.checkIn().format(BR_DATE) + " | Saída: " + r.checkOut().format(BR_DATE)
                + " | " + r.nights() + (r.nights() == 1 ? " diária" : " diárias"));
        lines.add("Diárias: " + Money.brl(r.subtotal()));
        if (r.discount().signum() > 0) {
            lines.add("Desconto: " + Money.brl(r.discount()));
        }
        lines.add("Total: " + Money.brl(r.total()));
        return String.join("\n", lines);
    }

    public static String confirmationBody(Receipt r) {
        return "Olá, " + r.guestName() + "!\n\n"
                + "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                + receiptText(r) + "\n\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                + "Até breve!";
    }

    static String formatCpf(String cpf) {
        if (cpf.matches("\\d{11}")) {
            return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." + cpf.substring(6, 9) + "-" + cpf.substring(9);
        }
        return cpf;
    }
}
