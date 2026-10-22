package hotel.web;

import hotel.reservations.*;
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
                guestCpf(reservation.guest()),
                companyCnpj(reservation.guest()),
                companyName(reservation.guest()),
                guestPassport(reservation.guest()),
                guestCountry(reservation.guest()),
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

    private static String guestCpf(Guest guest) {
        return guest instanceof IndividualGuest individual ? individual.cpf() : null;
    }

    private static String companyCnpj(Guest guest) {
        return guest instanceof CorporateGuest corporate ? corporate.cnpj() : null;
    }

    private static String companyName(Guest guest) {
        return guest instanceof CorporateGuest corporate ? corporate.companyName() : null;
    }

    private static String guestPassport(Guest guest) {
        return guest instanceof ForeignGuest foreign ? foreign.passport() : null;
    }

    private static String guestCountry(Guest guest) {
        return guest instanceof ForeignGuest foreign ? foreign.country() : null;
    }
}
