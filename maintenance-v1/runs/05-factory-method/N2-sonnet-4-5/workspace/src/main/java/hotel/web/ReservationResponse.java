package hotel.web;

import hotel.reservations.CorporateGuest;
import hotel.reservations.ForeignGuest;
import hotel.reservations.IndividualGuest;
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
                reservation.guest() instanceof IndividualGuest g ? g.cpf() : null,
                reservation.guest() instanceof CorporateGuest g ? g.companyCnpj() : null,
                reservation.guest() instanceof CorporateGuest g ? g.companyName() : null,
                reservation.guest() instanceof ForeignGuest g ? g.passportNumber() : null,
                reservation.guest() instanceof ForeignGuest g ? g.country() : null,
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
}
