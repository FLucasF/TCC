package hotel.documents;

import hotel.reservations.Identification;
import hotel.reservations.Reservation;
import hotel.rooms.Room;
import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * The document the guest gets when the reservation is confirmed, at check-out, and on request: a receipt, an
 * invoice or a booking receipt, according to how the guest is identified. The identification fields that do not
 * apply are null; text is the document as it goes in the e-mail.
 */
public record ReservationDocument(
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

    public static ReservationDocument of(Reservation reservation, Room room) {
        Identification identification = reservation.guest().identification();
        DocumentLayout layout = DocumentLayout.forGuest(identification);
        return new ReservationDocument(
                layout.title(),
                reservation.id(),
                reservation.guest().name(),
                identification.cpf(),
                identification.companyCnpj(),
                identification.companyName(),
                identification.passport(),
                identification.country(),
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
