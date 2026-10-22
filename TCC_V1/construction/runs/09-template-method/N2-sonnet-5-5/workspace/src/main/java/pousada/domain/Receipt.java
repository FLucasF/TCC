package pousada.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public record Receipt(String title, long reservationId, String guestName, String guestCpf, int roomNumber,
                      RoomType roomType, LocalDate checkIn, LocalDate checkOut, int nights,
                      BigDecimal subtotal, BigDecimal discount, BigDecimal total) {

    private static final String TITLE = "Comprovante de reserva";
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static Receipt of(Reservation r, Room room) {
        return new Receipt(TITLE, r.getId(), r.getGuestName(), r.getGuestCpf(), room.number(), room.type(),
                r.getCheckIn(), r.getCheckOut(), r.getNights(), r.getSubtotal(), r.getDiscount(), r.getTotal());
    }

    /** Texto do comprovante, usado também no corpo do e-mail. */
    public String text() {
        StringBuilder sb = new StringBuilder();
        sb.append(title).append(" nº ").append(reservationId).append('\n');
        sb.append("Hóspede: ").append(guestName).append(" (CPF ").append(formatCpf(guestCpf)).append(")\n");
        sb.append("Quarto: ").append(roomNumber).append(" (").append(roomType).append(")\n");
        sb.append("Entrada: ").append(DATE.format(checkIn)).append(" | Saída: ").append(DATE.format(checkOut))
                .append(" | ").append(nights).append(nights == 1 ? " diária" : " diárias").append('\n');
        sb.append("Diárias: ").append(Money.brl(subtotal)).append('\n');
        if (discount.signum() > 0) {
            sb.append("Desconto: ").append(Money.brl(discount)).append('\n');
        }
        sb.append("Total: ").append(Money.brl(total));
        return sb.toString();
    }

    private static String formatCpf(String cpf) {
        if (cpf.matches("\\d{11}")) {
            return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." + cpf.substring(6, 9) + "-" + cpf.substring(9);
        }
        return cpf;
    }
}
