package hotel.documents;

import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Unified document representation for JSON serialization. */
public record Document(
        String title,
        long reservationId,
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

    public static Document from(ReservationReceipt receipt) {
        return new Document(
                receipt.title(),
                receipt.reservationId(),
                receipt.guestName(),
                receipt.guestCpf(),
                receipt.companyCnpj(),
                receipt.companyName(),
                receipt.guestPassport(),
                receipt.guestCountry(),
                receipt.roomNumber(),
                receipt.roomType().name(),
                receipt.checkIn(),
                receipt.checkOut(),
                receipt.nights(),
                receipt.subtotal(),
                receipt.discount(),
                receipt.total(),
                receipt.text());
    }

    public static Document from(CompanyInvoice invoice) {
        return new Document(
                invoice.title(),
                invoice.reservationId(),
                invoice.guestName(),
                null,
                invoice.companyCnpj(),
                invoice.companyName(),
                null,
                null,
                invoice.roomNumber(),
                invoice.roomType().name(),
                invoice.checkIn(),
                invoice.checkOut(),
                invoice.nights(),
                invoice.subtotal(),
                invoice.discount(),
                invoice.total(),
                invoice.text());
    }

    public static Document from(InternationalBookingReceipt receipt) {
        return new Document(
                receipt.title(),
                receipt.reservationId(),
                receipt.guestName(),
                null,
                null,
                null,
                receipt.guestPassport(),
                receipt.guestCountry(),
                receipt.roomNumber(),
                receipt.roomType().name(),
                receipt.checkIn(),
                receipt.checkOut(),
                receipt.nights(),
                receipt.subtotal(),
                receipt.discount(),
                receipt.total(),
                receipt.text());
    }
}
