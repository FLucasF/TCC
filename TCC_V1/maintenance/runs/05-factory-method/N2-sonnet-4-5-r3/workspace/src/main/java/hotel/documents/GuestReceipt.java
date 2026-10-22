package hotel.documents;

import hotel.reservations.PersonGuest;
import hotel.reservations.Reservation;
import hotel.rooms.Room;
import java.math.BigDecimal;
import java.time.LocalDate;

/** The receipt for a guest with CPF (Brazilian individual). */
public record GuestReceipt(
        long reservationId,
        String guestName,
        String guestCpf,
        int roomNumber,
        String roomType,
        LocalDate checkIn,
        LocalDate checkOut,
        int nights,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal total) implements ReservationDocument {

    public GuestReceipt(Reservation reservation, Room room) {
        this(
                reservation.id(),
                reservation.guest().name(),
                ((PersonGuest) reservation.guest()).cpf(),
                room.number(),
                room.type().name(),
                reservation.checkIn(),
                reservation.checkOut(),
                reservation.price().nights(),
                reservation.price().subtotal(),
                reservation.price().discount(),
                reservation.price().total());
    }

    @Override
    public String title() {
        return "Comprovante de reserva";
    }

    @Override
    public String companyCnpj() {
        return null;
    }

    @Override
    public String companyName() {
        return null;
    }

    @Override
    public String guestPassport() {
        return null;
    }

    @Override
    public String guestCountry() {
        return null;
    }

    @Override
    public String text() {
        StringBuilder text = new StringBuilder();
        text.append(title()).append(" nº ").append(reservationId).append('\n');
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
