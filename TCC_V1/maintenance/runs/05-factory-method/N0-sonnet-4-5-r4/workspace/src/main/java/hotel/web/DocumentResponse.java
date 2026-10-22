package hotel.web;

import hotel.documents.ReservationDocument;
import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.time.LocalDate;

/** The JSON response for reservation documents (receipt, invoice, booking receipt). */
public record DocumentResponse(
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

    public DocumentResponse(ReservationDocument document) {
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
}
