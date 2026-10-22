package hotel.documents;

import hotel.reservations.Identification;
import hotel.reservations.Reservation;
import hotel.rooms.Room;
import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * The document the guest gets when the reservation is confirmed, at check-out, and on request: a receipt, an
 * invoice or a booking receipt, according to how the guest is identified.
 */
public record ReservationReceipt(
        String title,
        long reservationId,
        String guestName,
        String guestCpf,
        String companyCnpj,
        String companyName,
        String guestPassport,
        String guestCountry,
        int roomNumber,
        RoomType roomType,
        LocalDate checkIn,
        LocalDate checkOut,
        int nights,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal total,
        String text) {

    public ReservationReceipt(Reservation reservation, Room room) {
        this(reservation, room, DocumentLayouts.of(reservation.guest().identification()));
    }

    private ReservationReceipt(Reservation reservation, Room room, DocumentLayout layout) {
        this(
                layout.title(),
                reservation.id(),
                reservation.guest().name(),
                identification(reservation).cpf(),
                identification(reservation).companyCnpj(),
                identification(reservation).companyName(),
                identification(reservation).passport(),
                identification(reservation).country(),
                room.number(),
                room.type(),
                reservation.checkIn(),
                reservation.checkOut(),
                reservation.price().nights(),
                reservation.price().subtotal(),
                reservation.price().discount(),
                reservation.price().total(),
                layout.text(reservation, room));
    }

    private static Identification identification(Reservation reservation) {
        return reservation.guest().identification();
    }
}
