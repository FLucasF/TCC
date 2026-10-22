package hotel.documents;

import hotel.reservations.CompanyGuest;
import hotel.reservations.PriceCalculator;
import hotel.reservations.Reservation;
import hotel.rooms.Room;
import java.math.BigDecimal;
import java.time.LocalDate;

/** The invoice for a company booking (CNPJ). */
public record CompanyInvoice(
        long reservationId,
        String companyCnpj,
        String companyName,
        String guestName,
        int roomNumber,
        String roomType,
        LocalDate checkIn,
        LocalDate checkOut,
        int nights,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal total,
        BigDecimal issWithheld) implements ReservationDocument {

    private static final BigDecimal ISS_RATE = new BigDecimal("0.05");

    public CompanyInvoice(Reservation reservation, Room room) {
        this(
                reservation.id(),
                ((CompanyGuest) reservation.guest()).cnpj(),
                ((CompanyGuest) reservation.guest()).companyName(),
                reservation.guest().name(),
                room.number(),
                room.type().name(),
                reservation.checkIn(),
                reservation.checkOut(),
                reservation.price().nights(),
                reservation.price().subtotal(),
                reservation.price().discount(),
                reservation.price().total(),
                PriceCalculator.cents(reservation.price().total().multiply(ISS_RATE)));
    }

    @Override
    public String title() {
        return "Fatura de hospedagem";
    }

    @Override
    public String guestCpf() {
        return null;
    }

    @Override
    public String guestPassport() {
        return null;
    }

    @Override
    public String guestCountry() {
        return null;
    }

    @Override
    public String text() {
        StringBuilder text = new StringBuilder();
        text.append(title()).append(" nº ").append(reservationId).append('\n');
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
