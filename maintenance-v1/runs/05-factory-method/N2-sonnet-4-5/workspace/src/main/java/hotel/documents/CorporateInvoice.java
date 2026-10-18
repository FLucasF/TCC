package hotel.documents;

import hotel.reservations.CorporateGuest;
import hotel.reservations.PriceCalculator;
import hotel.reservations.Reservation;
import hotel.rooms.Room;
import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Invoice for corporate bookings (company identified by CNPJ). */
public record CorporateInvoice(
        String title,
        long reservationId,
        String guestName,
        String companyCnpj,
        String companyName,
        int roomNumber,
        RoomType roomType,
        LocalDate checkIn,
        LocalDate checkOut,
        int nights,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal total,
        BigDecimal issWithheld) implements ReservationDocument {

    public CorporateInvoice(Reservation reservation, Room room, CorporateGuest guest) {
        this(
                "Fatura de hospedagem",
                reservation.id(),
                guest.name(),
                guest.companyCnpj(),
                guest.companyName(),
                room.number(),
                room.type(),
                reservation.checkIn(),
                reservation.checkOut(),
                reservation.price().nights(),
                reservation.price().subtotal(),
                reservation.price().discount(),
                reservation.price().total(),
                calculateIssWithheld(reservation.price().total()));
    }

    private static BigDecimal calculateIssWithheld(BigDecimal total) {
        return PriceCalculator.cents(total.multiply(new BigDecimal("0.05")));
    }

    @Override
    public String text() {
        StringBuilder text = new StringBuilder();
        text.append(title).append(" nº ").append(reservationId).append('\n');
        text.append("Empresa: ").append(companyName).append(" (CNPJ ").append(companyCnpj).append(")\n");
        text.append("Hóspede: ").append(guestName).append('\n');
        text.append("Quarto: ").append(roomNumber).append(" (").append(roomType).append(")\n");
        text.append("Entrada: ").append(TextFormat.date(checkIn))
                .append(" | Saída: ").append(TextFormat.date(checkOut))
                .append(" | ").append(nights).append(nights == 1 ? " diária" : " diárias").append('\n');
        text.append("Diárias: ").append(TextFormat.money(subtotal)).append('\n');
        if (discount.signum() > 0) {
            text.append("Desconto: ").append(TextFormat.money(discount)).append('\n');
        }
        text.append("Total: ").append(TextFormat.money(total)).append('\n');
        text.append("ISS retido (5%): ").append(TextFormat.money(issWithheld)).append('\n');
        return text.toString();
    }
}
