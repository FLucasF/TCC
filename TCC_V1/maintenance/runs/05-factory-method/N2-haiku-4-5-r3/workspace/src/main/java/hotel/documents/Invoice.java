package hotel.documents;

import hotel.reservations.Reservation;
import hotel.reservations.PriceCalculator;
import hotel.rooms.Room;
import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Invoice for company bookings (CNPJ). */
public record Invoice(
        long reservationId,
        String companyName,
        String companyCnpj,
        String guestName,
        int roomNumber,
        RoomType roomType,
        LocalDate checkIn,
        LocalDate checkOut,
        int nights,
        BigDecimal subtotal,
        BigDecimal total) implements Document {

    public Invoice(Reservation reservation, Room room) {
        this(
                reservation.id(),
                reservation.guest().companyName(),
                reservation.guest().companyCnpj(),
                reservation.guest().name(),
                room.number(),
                room.type(),
                reservation.checkIn(),
                reservation.checkOut(),
                reservation.price().nights(),
                reservation.price().subtotal(),
                reservation.price().total());
    }

    public String text() {
        BigDecimal issRetained = calculateIssRetained(total);
        StringBuilder text = new StringBuilder();
        text.append("Fatura de hospedagem nº ").append(reservationId).append('\n');
        text.append("Empresa: ").append(companyName).append(" (CNPJ ").append(companyCnpj).append(")\n");
        text.append("Hóspede: ").append(guestName).append('\n');
        text.append("Quarto: ").append(roomNumber).append(" (").append(roomType).append(")\n");
        text.append("Entrada: ").append(TextFormat.datePtBr(checkIn))
                .append(" | Saída: ").append(TextFormat.datePtBr(checkOut))
                .append(" | ").append(nights).append(nights == 1 ? " diária" : " diárias").append('\n');
        text.append("Diárias: ").append(TextFormat.moneyPtBr(subtotal)).append('\n');
        text.append("Total: ").append(TextFormat.moneyPtBr(total)).append('\n');
        text.append("ISS retido (5%): ").append(TextFormat.moneyPtBr(issRetained)).append('\n');
        return text.toString();
    }

    private static BigDecimal calculateIssRetained(BigDecimal total) {
        BigDecimal iss = total.multiply(new BigDecimal("0.05"));
        return iss.setScale(2, java.math.RoundingMode.HALF_EVEN);
    }
}
