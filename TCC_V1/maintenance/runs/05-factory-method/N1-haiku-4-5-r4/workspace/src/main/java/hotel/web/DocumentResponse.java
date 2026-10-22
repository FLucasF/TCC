package hotel.web;

import hotel.documents.Document;
import java.math.BigDecimal;
import java.time.LocalDate;

public record DocumentResponse(
        long reservationId,
        String title,
        String guestName,
        String guestCpf,
        String companyCnpj,
        String companyName,
        String guestPassport,
        String guestCountry,
        int roomNumber,
        String roomType,
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
                document.title(),
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
