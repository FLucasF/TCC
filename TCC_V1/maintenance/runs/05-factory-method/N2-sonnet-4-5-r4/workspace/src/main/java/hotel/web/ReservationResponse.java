package hotel.web;

import hotel.reservations.CorporateIdentity;
import hotel.reservations.ForeignIdentity;
import hotel.reservations.IndividualIdentity;
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
                extractCpf(reservation),
                extractCnpj(reservation),
                extractCompanyName(reservation),
                extractPassport(reservation),
                extractCountry(reservation),
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

    private static String extractCpf(Reservation reservation) {
        return reservation.guest().identity() instanceof IndividualIdentity i ? i.cpf() : null;
    }

    private static String extractCnpj(Reservation reservation) {
        return reservation.guest().identity() instanceof CorporateIdentity c ? c.cnpj() : null;
    }

    private static String extractCompanyName(Reservation reservation) {
        return reservation.guest().identity() instanceof CorporateIdentity c ? c.companyName() : null;
    }

    private static String extractPassport(Reservation reservation) {
        return reservation.guest().identity() instanceof ForeignIdentity f ? f.passport() : null;
    }

    private static String extractCountry(Reservation reservation) {
        return reservation.guest().identity() instanceof ForeignIdentity f ? f.country() : null;
    }
}
