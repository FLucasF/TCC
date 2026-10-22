package hotel.documents;

import hotel.reservations.Reservation;
import hotel.rooms.Room;
import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.time.LocalDate;

/** The receipt the guest gets when the reservation is confirmed, at check-out, and on request. */
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
        this(reservation, room, selectGenerator(reservation));
    }

    private ReservationReceipt(Reservation reservation, Room room, DocumentGenerator generator) {
        this(
                generator.title(),
                reservation.id(),
                reservation.guest().name(),
                reservation.guest().cpf(),
                reservation.guest().companyCnpj(),
                reservation.guest().companyName(),
                reservation.guest().passport(),
                reservation.guest().country(),
                room.number(),
                room.type(),
                reservation.checkIn(),
                reservation.checkOut(),
                reservation.price().nights(),
                reservation.price().subtotal(),
                reservation.price().discount(),
                reservation.price().total(),
                generator.formatDocument(
                        reservation.id(),
                        reservation.guest(),
                        room,
                        reservation.checkIn(),
                        reservation.checkOut(),
                        reservation.price()));
    }

    private static DocumentGenerator selectGenerator(Reservation reservation) {
        if (reservation.guest().cpf() != null) {
            return new IndividualDocumentGenerator();
        }
        if (reservation.guest().companyCnpj() != null) {
            return new CompanyDocumentGenerator();
        }
        return new ForeignDocumentGenerator();
    }
}

