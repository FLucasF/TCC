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
                extractCpf(reservation.guest().identification()),
                extractCompanyCnpj(reservation.guest().identification()),
                extractCompanyName(reservation.guest().identification()),
                extractPassport(reservation.guest().identification()),
                extractCountry(reservation.guest().identification()),
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

    private static String extractCpf(Identification id) {
        return id instanceof Identification.CpfIdentification cpf ? cpf.cpf() : null;
    }

    private static String extractCompanyCnpj(Identification id) {
        return id instanceof Identification.CompanyIdentification company ? company.cnpj() : null;
    }

    private static String extractCompanyName(Identification id) {
        return id instanceof Identification.CompanyIdentification company ? company.companyName() : null;
    }

    private static String extractPassport(Identification id) {
        return id instanceof Identification.PassportIdentification passport ? passport.passport() : null;
    }

    private static String extractCountry(Identification id) {
        return id instanceof Identification.PassportIdentification passport ? passport.country() : null;
    }
}
