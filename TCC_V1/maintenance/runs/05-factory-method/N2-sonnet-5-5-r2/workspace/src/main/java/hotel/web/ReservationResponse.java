package hotel.web;

import hotel.reservations.IdentificationFields;
import hotel.reservations.RatePlan;
import hotel.reservations.Reservation;
import hotel.reservations.ReservationStatus;
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
        BigDecimal refund) {

    public ReservationResponse(Reservation reservation) {
        this(
                reservation.id(),
                reservation.roomNumber(),
                reservation.guest().name(),
                reservation.guest().email(),
                identificationOf(reservation).cpf(),
                identificationOf(reservation).companyCnpj(),
                identificationOf(reservation).companyName(),
                identificationOf(reservation).guestPassport(),
                identificationOf(reservation).guestCountry(),
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
                reservation.refund());
    }

    private static IdentificationFields identificationOf(Reservation reservation) {
        return reservation.guest().identification().fields();
    }
}
