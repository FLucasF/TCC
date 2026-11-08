package br.com.marealta.domain;

import java.time.format.DateTimeFormatter;

public final class ReceiptFormatter {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private ReceiptFormatter() {}

    public static String text(Receipt r) {
        StringBuilder sb = new StringBuilder();
        sb.append("Comprovante de reserva nº ").append(r.reservationId()).append('\n');
        sb.append("Hóspede: ").append(r.guestName()).append(" (CPF ").append(r.guestCpf()).append(")\n");
        sb.append("Quarto: ").append(r.roomNumber()).append(" (").append(r.roomType()).append(")\n");
        sb.append("Entrada: ").append(DATE.format(r.checkIn()))
                .append(" | Saída: ").append(DATE.format(r.checkOut()))
                .append(" | ").append(r.nights()).append(r.nights() == 1 ? " diária" : " diárias").append('\n');
        sb.append("Diárias: ").append(Money.brl(r.subtotal())).append('\n');
        if (r.discount().signum() > 0) {
            sb.append("Desconto: ").append(Money.brl(r.discount())).append('\n');
        }
        sb.append("Total: ").append(Money.brl(r.total()));
        return sb.toString();
    }
}
