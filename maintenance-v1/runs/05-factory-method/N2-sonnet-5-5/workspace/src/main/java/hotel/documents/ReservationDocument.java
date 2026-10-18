package hotel.documents;

import hotel.reservations.Identification;
import hotel.reservations.Reservation;
import hotel.rooms.Room;
import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * The document the guest gets when the reservation is confirmed, at check-out, and on request. Which
 * document it is (receipt, invoice, booking receipt) depends on how the guest is identified.
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

    static ReservationDocument issue(Reservation reservation, Room room, DocumentLayout layout) {
        Identification identification = reservation.guest().identification();
        ReservationDocument draft = new ReservationDocument(
                layout.title(),
                reservation.id(),
                reservation.guest().name(),
                identification.cpf(),
                identification.cnpj(),
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
                null);
        return draft.withText(layout.text(draft));
    }

    private ReservationDocument withText(String newText) {
        return new ReservationDocument(
                title, reservationId, guestName, guestCpf, companyCnpj, companyName, guestPassport, guestCountry,
                roomNumber, roomType, checkIn, checkOut, nights, subtotal, discount, total, newText);
    }
}
