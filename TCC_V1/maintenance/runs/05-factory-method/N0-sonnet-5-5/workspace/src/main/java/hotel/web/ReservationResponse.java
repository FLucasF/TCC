package hotel.web;

import hotel.reservations.Identification;
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
                cpf(reservation),
                cnpj(reservation),
                companyName(reservation),
                passport(reservation),
                country(reservation),
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

    private static String cpf(Reservation r) {
        return r.guest().identification() instanceof Identification.Cpf c ? c.number() : null;
    }

    private static String cnpj(Reservation r) {
        return r.guest().identification() instanceof Identification.Company c ? c.cnpj() : null;
    }

    private static String companyName(Reservation r) {
        return r.guest().identification() instanceof Identification.Company c ? c.name() : null;
    }

    private static String passport(Reservation r) {
        return r.guest().identification() instanceof Identification.Foreign f ? f.passport() : null;
    }

    private static String country(Reservation r) {
        return r.guest().identification() instanceof Identification.Foreign f ? f.country() : null;
    }
}
