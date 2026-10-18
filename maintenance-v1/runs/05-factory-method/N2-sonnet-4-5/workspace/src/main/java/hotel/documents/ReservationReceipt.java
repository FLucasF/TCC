package hotel.documents;

import hotel.reservations.Reservation;
import hotel.rooms.Room;
import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * The receipt the guest gets when the reservation is confirmed, at check-out, and on request.
 * This is a unified DTO that contains all fields from all document types. Fields that don't apply
 * to the document type are null.
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
        this(ReservationDocument.create(reservation, room));
    }

    public ReservationReceipt(ReservationDocument document) {
        this(
                document.title(),
                document.reservationId(),
                document.guestName(),
                document instanceof GuestReceipt r ? r.guestCpf() : null,
                document instanceof CorporateInvoice i ? i.companyCnpj() : null,
                document instanceof CorporateInvoice i ? i.companyName() : null,
                document instanceof BookingReceipt b ? b.passportNumber() : null,
                document instanceof BookingReceipt b ? b.country() : null,
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

    public String text() {
        return text;
    }
}
