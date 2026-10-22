package hotel.documents;

import hotel.reservations.Reservation;
import hotel.rooms.Room;
import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.time.LocalDate;

public final class CnpjInvoice extends ReservationReceipt {

    private final String companyCnpj;
    private final String companyName;

    public CnpjInvoice(
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
            BigDecimal total) {
        super(reservationId, guestName, roomNumber, roomType, checkIn, checkOut, nights, subtotal, discount, total);
        this.companyCnpj = companyCnpj;
        this.companyName = companyName;
    }

    public CnpjInvoice(Reservation reservation, Room room) {
        this(
                reservation.id(),
                reservation.guest().name(),
                reservation.guest().companyCnpj(),
                reservation.guest().companyName(),
                room.number(),
                room.type(),
                reservation.checkIn(),
                reservation.checkOut(),
                reservation.price().nights(),
                reservation.price().subtotal(),
                reservation.price().discount(),
                reservation.price().total());
    }

    public String companyCnpj() {
        return companyCnpj;
    }

    public String companyName() {
        return companyName;
    }

    @Override
    public String title() {
        return "Fatura de hospedagem";
    }

    private BigDecimal calculateIss() {
        return total.multiply(new BigDecimal("0.05")).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    @Override
    public String text() {
        StringBuilder text = new StringBuilder();
        text.append("Fatura de hospedagem nº ").append(reservationId).append('\n');
        text.append("Empresa: ").append(companyName).append(" (CNPJ ").append(companyCnpj).append(")\n");
        text.append("Hóspede: ").append(guestName).append("\n");
        text.append("Quarto: ").append(roomNumber).append(" (").append(roomType).append(")\n");
        text.append("Entrada: ").append(TextFormat.date(checkIn))
                .append(" | Saída: ").append(TextFormat.date(checkOut))
                .append(" | ").append(nights).append(nights == 1 ? " diária" : " diárias").append('\n');
        text.append("Diárias: ").append(TextFormat.money(subtotal)).append('\n');
        if (discount.signum() > 0) {
            text.append("Desconto: ").append(TextFormat.money(discount)).append('\n');
        }
        text.append("Total: ").append(TextFormat.money(total)).append('\n');
        BigDecimal iss = calculateIss();
        text.append("ISS retido (5%): ").append(TextFormat.money(iss)).append('\n');
        return text.toString();
    }
}
