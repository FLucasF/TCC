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
        this(ReservationDocument.of(reservation, room));
    }

    private ReservationReceipt(ReservationDocument document) {
        this(
                document.title(),
                document.reservationId(),
                document.guestName(),
                document.guestCpf(),
                document.companyCnpj(),
                document.companyName(),
                document.guestPassport(),
                document.guestCountry(),
                document.roomNumber(),
                document.roomType(),
                document.checkIn(),
                document.checkOut(),
                document.nights(),
                document.subtotal(),
                document.discount(),
                document.total(),
                document.text());
    }

    /** @deprecated Use the text field instead. */
    @Deprecated
    public String text() {
        return text;
    }
}
