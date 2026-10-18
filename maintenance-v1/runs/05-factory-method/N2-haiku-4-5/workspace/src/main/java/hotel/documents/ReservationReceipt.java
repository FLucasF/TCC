package hotel.documents;

import hotel.reservations.Identification;
import hotel.reservations.Reservation;
import hotel.rooms.Room;
import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ReservationReceipt(
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

    public ReservationReceipt(Reservation reservation, Room room) {
        this(
                reservation.id(),
                reservation.guest().name(),
                extractCpf(reservation.guest().identification()),
                extractCompanyCnpj(reservation.guest().identification()),
                extractCompanyName(reservation.guest().identification()),
                extractPassport(reservation.guest().identification()),
                extractCountry(reservation.guest().identification()),
                room.number(),
                room.type(),
                reservation.checkIn(),
                reservation.checkOut(),
                reservation.price().nights(),
                reservation.price().subtotal(),
                reservation.price().discount(),
                reservation.price().total(),
                generateText(reservation.guest().identification(), reservation.id(),
                        reservation.guest().name(), room.number(), room.type(),
                        reservation.checkIn(), reservation.checkOut(),
                        reservation.price().nights(), reservation.price().subtotal(),
                        reservation.price().discount(), reservation.price().total()));
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

    private static String generateText(Identification identification, long reservationId, String guestName,
            int roomNumber, RoomType roomType, LocalDate checkIn, LocalDate checkOut, int nights,
            BigDecimal subtotal, BigDecimal discount, BigDecimal total) {
        DocumentText doc = DocumentText.create(identification, reservationId, guestName, roomNumber, roomType,
                checkIn, checkOut, nights, subtotal, discount, total);
        return doc.text();
    }
}
