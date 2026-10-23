package hotel.documents;

import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.time.LocalDate;

/** The JSON representation of a reservation document. */
public record DocumentResponse(
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
        BigDecimal iss,
        String text) {

    public DocumentResponse(ReservationReceipt receipt) {
        this(
                receipt.reservationId(),
                receipt.guestName(),
                receipt.guestCpf(),
                receipt.companyCnpj(),
                receipt.companyName(),
                receipt.guestPassport(),
                receipt.guestCountry(),
                receipt.roomNumber(),
                receipt.roomType(),
                receipt.checkIn(),
                receipt.checkOut(),
                receipt.nights(),
                receipt.subtotal(),
                receipt.discount(),
                receipt.total(),
                receipt.iss(),
                receipt.text());
    }
}
