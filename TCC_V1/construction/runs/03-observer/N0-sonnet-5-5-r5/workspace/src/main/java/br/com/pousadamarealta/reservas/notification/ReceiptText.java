package br.com.pousadamarealta.reservas.notification;

import br.com.pousadamarealta.reservas.domain.Receipt;

public final class ReceiptText {
    private ReceiptText() {}

    public static String of(Receipt r) {
        StringBuilder sb = new StringBuilder();
        sb.append(r.title()).append(" nº ").append(r.reservationId()).append('\n');
        sb.append("Hóspede: ").append(r.guestName()).append(" (CPF ").append(Brl.cpf(r.guestCpf())).append(")\n");
        sb.append("Quarto: ").append(r.roomNumber()).append(" (").append(r.roomType()).append(")\n");
        sb.append("Entrada: ").append(Brl.date(r.checkIn())).append(" | Saída: ").append(Brl.date(r.checkOut()))
                .append(" | ").append(r.nights()).append(r.nights() == 1 ? " diária" : " diárias").append('\n');
        sb.append("Diárias: ").append(Brl.money(r.subtotal())).append('\n');
        if (r.discount().signum() > 0) {
            sb.append("Desconto: ").append(Brl.money(r.discount())).append('\n');
        }
        sb.append("Total: ").append(Brl.money(r.total()));
        return sb.toString();
    }
}
