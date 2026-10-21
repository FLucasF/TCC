package br.com.pousadamarealta;

/** Builds the text of the reservation receipt (comprovante). */
public final class Receipt {
    private Receipt() {}

    public static String text(Reservation r) {
        StringBuilder sb = new StringBuilder();
        sb.append("Comprovante de reserva nº ").append(r.id).append('\n');
        sb.append("Hóspede: ").append(r.guestName).append(" (CPF ").append(r.guestCpf).append(")\n");
        sb.append("Quarto: ").append(r.room.number()).append(" (").append(r.room.type()).append(")\n");
        sb.append("Entrada: ").append(Money.brDate(r.checkIn)).append(" | Saída: ")
                .append(Money.brDate(r.checkOut)).append(" | ")
                .append(r.nights).append(r.nights == 1 ? " diária" : " diárias").append('\n');
        sb.append("Diárias: ").append(Money.brl(r.subtotal)).append('\n');
        if (r.discount.signum() > 0) {
            sb.append("Desconto: ").append(Money.brl(r.discount)).append('\n');
        }
        sb.append("Total: ").append(Money.brl(r.total));
        return sb.toString();
    }
}
