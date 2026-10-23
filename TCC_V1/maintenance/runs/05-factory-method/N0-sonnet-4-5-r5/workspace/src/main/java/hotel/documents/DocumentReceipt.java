package hotel.documents;

import hotel.reservations.Reservation;
import hotel.rooms.Room;
import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Unified document for all types of reservations. */
public record DocumentReceipt(
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

    public static DocumentReceipt of(Reservation reservation, Room room) {
        return switch (reservation.guest().type()) {
            case INDIVIDUAL -> fromReservationReceipt(new ReservationReceipt(reservation, room));
            case COMPANY -> fromInvoiceReceipt(new InvoiceReceipt(reservation, room));
            case FOREIGN -> fromBookingReceipt(new BookingReceipt(reservation, room));
        };
    }

    private static DocumentReceipt fromReservationReceipt(ReservationReceipt receipt) {
        return new DocumentReceipt(
                receipt.title(),
                receipt.reservationId(),
                receipt.guestName(),
                receipt.guestCpf(),
                null,
                null,
                null,
                null,
                receipt.roomNumber(),
                receipt.roomType(),
                receipt.checkIn(),
                receipt.checkOut(),
                receipt.nights(),
                receipt.subtotal(),
                receipt.discount(),
                receipt.total(),
                receipt.text());
    }

    private static DocumentReceipt fromInvoiceReceipt(InvoiceReceipt receipt) {
        return new DocumentReceipt(
                receipt.title(),
                receipt.reservationId(),
                receipt.guestName(),
                null,
                receipt.companyCnpj(),
                receipt.companyName(),
                null,
                null,
                receipt.roomNumber(),
                receipt.roomType(),
                receipt.checkIn(),
                receipt.checkOut(),
                receipt.nights(),
                receipt.subtotal(),
                receipt.discount(),
                receipt.total(),
                receipt.text());
    }

    private static DocumentReceipt fromBookingReceipt(BookingReceipt receipt) {
        return new DocumentReceipt(
                receipt.title(),
                receipt.reservationId(),
                receipt.guestName(),
                null,
                null,
                null,
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
                receipt.text());
    }
}
