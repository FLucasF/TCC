package hotel.documents;

import hotel.reservations.Reservation;
import hotel.rooms.Room;
import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * The document the guest receives when a reservation is confirmed, at check-out, and on request.
 * The format depends on the guest type: guest receipt for individuals, corporate invoice for companies,
 * and booking receipt for foreign guests.
 */
public sealed interface ReservationDocument permits GuestReceipt, CorporateInvoice, BookingReceipt {

    String title();
    long reservationId();
    String guestName();
    int roomNumber();
    RoomType roomType();
    LocalDate checkIn();
    LocalDate checkOut();
    int nights();
    BigDecimal subtotal();
    BigDecimal discount();
    BigDecimal total();
    String text();

    /** Creates the appropriate document for the reservation. */
    static ReservationDocument create(Reservation reservation, Room room) {
        return switch (reservation.guest()) {
            case hotel.reservations.IndividualGuest individual ->
                new GuestReceipt(reservation, room, individual);
            case hotel.reservations.CorporateGuest corporate ->
                new CorporateInvoice(reservation, room, corporate);
            case hotel.reservations.ForeignGuest foreign ->
                new BookingReceipt(reservation, room, foreign);
        };
    }
}
