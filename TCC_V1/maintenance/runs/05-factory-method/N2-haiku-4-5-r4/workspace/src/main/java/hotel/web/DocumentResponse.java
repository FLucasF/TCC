package hotel.web;

import hotel.documents.Document;
import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.time.LocalDate;

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
        String text) {

    public DocumentResponse(Document document) {
        this(
                document.reservationId(),
                null,
                null,
                null,
                null,
                null,
                null,
                0,
                null,
                null,
                null,
                0,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                document.text());
    }
}
