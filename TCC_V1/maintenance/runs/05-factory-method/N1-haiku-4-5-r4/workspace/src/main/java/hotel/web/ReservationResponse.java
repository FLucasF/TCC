package hotel.web;

import hotel.documents.Document;
import hotel.reservations.RatePlan;
import hotel.reservations.Reservation;
import hotel.reservations.ReservationStatus;
import hotel.rooms.Rooms;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ReservationResponse(
        long id,
        int roomNumber,
        String guestName,
        String guestEmail,
        String guestCpf,
        String companyCnpj,
        String companyName,
        String guestPassport,
        String guestCountry,
        LocalDate checkIn,
        LocalDate checkOut,
        int guests,
        RatePlan ratePlan,
        int nights,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal total,
        ReservationStatus status,
        BigDecimal amountPaid,
        BigDecimal refund,
        String text) {

    public ReservationResponse(Reservation reservation, Rooms rooms) {
        this(
                reservation.id(),
                reservation.roomNumber(),
                reservation.guest().name(),
                reservation.guest().email(),
                reservation.guest().cpf(),
                reservation.guest().companyCnpj(),
                reservation.guest().companyName(),
                reservation.guest().guestPassport(),
                reservation.guest().guestCountry(),
                reservation.checkIn(),
                reservation.checkOut(),
                reservation.guests(),
                reservation.ratePlan(),
                reservation.price().nights(),
                reservation.price().subtotal(),
                reservation.price().discount(),
                reservation.price().total(),
                reservation.status(),
                reservation.amountPaid(),
                reservation.refund(),
                Document.of(reservation, rooms.find(reservation.roomNumber()).orElseThrow()).text());
    }
}
