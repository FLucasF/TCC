package hotel.documents;

import hotel.reservations.Reservation;
import hotel.rooms.Room;
import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.time.LocalDate;

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

    public static ReservationDocument from(Reservation reservation, Room room) {
        if (reservation.guest().isIndividual()) {
            ReservationReceipt receipt = new ReservationReceipt(reservation, room);
            return new ReservationDocument(
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
        } else if (reservation.guest().isCompany()) {
            HostingInvoice invoice = new HostingInvoice(reservation, room);
            return new ReservationDocument(
                    invoice.title(),
                    invoice.reservationId(),
                    invoice.guestName(),
                    null,
                    invoice.companyCnpj(),
                    invoice.companyName(),
                    null,
                    null,
                    invoice.roomNumber(),
                    invoice.roomType(),
                    invoice.checkIn(),
                    invoice.checkOut(),
                    invoice.nights(),
                    invoice.subtotal(),
                    invoice.discount(),
                    invoice.total(),
                    invoice.text());
        } else {
            InternationalReceipt receipt = new InternationalReceipt(reservation, room);
            return new ReservationDocument(
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
}
