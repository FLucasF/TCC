package br.com.marealta.service;

import br.com.marealta.domain.Money;

import java.time.format.DateTimeFormatter;

public final class ReceiptFormatter {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private ReceiptFormatter() {}

    public static String text(Receipt r) {
        StringBuilder text = new StringBuilder()
                .append(r.title()).append(" nº ").append(r.reservationId()).append('\n')
                .append("Hóspede: ").append(r.guestName()).append(" (CPF ").append(r.guestCpf()).append(")\n")
                .append("Quarto: ").append(r.roomNumber()).append(" (").append(r.roomType()).append(")\n")
                .append("Entrada: ").append(DATE.format(r.checkIn()))
                .append(" | Saída: ").append(DATE.format(r.checkOut()))
                .append(" | ").append(r.nights()).append(r.nights() == 1 ? " diária" : " diárias").append('\n')
                .append("Diárias: ").append(Money.format(r.subtotal())).append('\n');
        if (r.discount().signum() > 0) {
            text.append("Desconto: ").append(Money.format(r.discount())).append('\n');
        }
        return text.append("Total: ").append(Money.format(r.total())).toString();
    }

    public static String confirmationEmail(String guestName, Receipt receipt) {
        return "Olá, " + guestName + "!\n\n"
                + "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                + text(receipt) + "\n\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                + "Até breve!";
    }
}
