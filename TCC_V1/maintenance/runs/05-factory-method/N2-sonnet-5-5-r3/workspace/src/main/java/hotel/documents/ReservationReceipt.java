package hotel.documents;

import hotel.reservations.Reservation;
import hotel.rooms.Room;
import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * The document the guest gets when the reservation is confirmed, at check-out, and on request: a receipt, an invoice
 * or a booking receipt, according to how the guest is identified. The identification fields that do not apply are null.
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
        this(
                reservation.guest().identification().layout(),
                reservation,
                room);
    }

    private ReservationReceipt(DocumentLayout layout, Reservation reservation, Room room) {
        this(
                layout.title(),
                reservation.id(),
                reservation.guest().name(),
                reservation.guest().identification().cpf(),
                reservation.guest().identification().companyCnpj(),
                reservation.guest().identification().companyName(),
                reservation.guest().identification().passport(),
                reservation.guest().identification().country(),
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
}
