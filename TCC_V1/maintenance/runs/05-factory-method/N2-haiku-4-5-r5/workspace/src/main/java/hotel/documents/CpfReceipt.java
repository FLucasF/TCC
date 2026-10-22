package hotel.documents;

import hotel.reservations.Reservation;
import hotel.rooms.Room;
import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.time.LocalDate;

public final class CpfReceipt extends ReservationReceipt {

    private final String guestCpf;

    public CpfReceipt(
            long reservationId,
            String guestName,
            String guestCpf,
            int roomNumber,
            RoomType roomType,
            LocalDate checkIn,
            LocalDate checkOut,
            int nights,
            BigDecimal subtotal,
            BigDecimal discount,
            BigDecimal total) {
        super(reservationId, guestName, roomNumber, roomType, checkIn, checkOut, nights, subtotal, discount, total);
        this.guestCpf = guestCpf;
    }

    public CpfReceipt(Reservation reservation, Room room) {
        this(
                reservation.id(),
                reservation.guest().name(),
                reservation.guest().cpf(),
                room.number(),
                room.type(),
                reservation.checkIn(),
                reservation.checkOut(),
                reservation.price().nights(),
                reservation.price().subtotal(),
                reservation.price().discount(),
                reservation.price().total());
    }

    public String guestCpf() {
        return guestCpf;
    }

    @Override
    public String title() {
        return "Comprovante de reserva";
    }

    @Override
    public String text() {
        StringBuilder text = new StringBuilder();
        text.append("Comprovante de reserva nº ").append(reservationId).append('\n');
        text.append("Hóspede: ").append(guestName).append(" (CPF ").append(guestCpf).append(")\n");
        text.append("Quarto: ").append(roomNumber).append(" (").append(roomType).append(")\n");
        text.append("Entrada: ").append(TextFormat.date(checkIn))
                .append(" | Saída: ").append(TextFormat.date(checkOut))
                .append(" | ").append(nights).append(nights == 1 ? " diária" : " diárias").append('\n');
        text.append("Diárias: ").append(TextFormat.money(subtotal)).append('\n');
        if (discount.signum() > 0) {
            text.append("Desconto: ").append(TextFormat.money(discount)).append('\n');
        }
        text.append("Total: ").append(TextFormat.money(total)).append('\n');
        return text.toString();
    }
}
