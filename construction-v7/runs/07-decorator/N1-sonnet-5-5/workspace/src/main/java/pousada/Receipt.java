package pousada;

import java.math.BigDecimal;

public record Receipt(String title, int reservationId, String guestName, String guestCpf, int roomNumber,
                      RoomType roomType, String checkIn, String checkOut, int nights, BigDecimal subtotal,
                      BigDecimal discount, BigDecimal total) {

    public static final String TITLE = "Comprovante de reserva";

    public static Receipt of(Reservation r) {
        return new Receipt(TITLE, r.id, r.guestName, r.guestCpf, r.room.number(), r.room.type(),
                r.checkIn.toString(), r.checkOut.toString(), r.nights, r.subtotal, r.discount, r.total);
    }

    /** Texto do comprovante, usado também no corpo do e-mail de confirmação. */
    public static String text(Reservation r) {
        StringBuilder sb = new StringBuilder();
        sb.append(TITLE).append(" nº ").append(r.id).append('\n');
        sb.append("Hóspede: ").append(r.guestName).append(" (CPF ").append(r.guestCpf).append(")\n");
        sb.append("Quarto: ").append(r.room.number()).append(" (").append(r.room.type()).append(")\n");
        sb.append("Entrada: ").append(Money.brDate(r.checkIn)).append(" | Saída: ").append(Money.brDate(r.checkOut))
                .append(" | ").append(r.nights).append(r.nights == 1 ? " diária" : " diárias").append('\n');
        sb.append("Diárias: ").append(Money.brl(r.subtotal)).append('\n');
        if (r.discount.signum() > 0) {
            sb.append("Desconto: ").append(Money.brl(r.discount)).append('\n');
        }
        sb.append("Total: ").append(Money.brl(r.total));
        return sb.toString();
    }
}
