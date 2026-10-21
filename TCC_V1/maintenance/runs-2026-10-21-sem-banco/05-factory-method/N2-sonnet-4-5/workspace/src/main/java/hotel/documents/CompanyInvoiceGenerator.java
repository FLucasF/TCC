package hotel.documents;

import hotel.reservations.PriceCalculator;
import hotel.reservations.Reservation;
import hotel.rooms.Room;
import java.math.BigDecimal;

/** Generates the invoice for company bookings in Portuguese, including ISS. */
public final class CompanyInvoiceGenerator implements DocumentGenerator {

    private static final BigDecimal ISS_RATE = new BigDecimal("0.05");

    @Override
    public ReservationReceipt generate(Reservation reservation, Room room) {
        String text = buildText(reservation, room);
        return new ReservationReceipt(
                "Fatura de hospedagem",
                reservation.id(),
                reservation.guest().name(),
                null,
                reservation.guest().companyCnpj(),
                reservation.guest().companyName(),
                null,
                null,
                room.number(),
                room.type(),
                reservation.checkIn(),
                reservation.checkOut(),
                reservation.price().nights(),
                reservation.price().subtotal(),
                reservation.price().discount(),
                reservation.price().total(),
                text);
    }

    private String buildText(Reservation reservation, Room room) {
        BigDecimal iss = PriceCalculator.cents(reservation.price().total().multiply(ISS_RATE));
        StringBuilder text = new StringBuilder();
        text.append("Fatura de hospedagem nº ").append(reservation.id()).append('\n');
        text.append("Empresa: ")
                .append(reservation.guest().companyName())
                .append(" (CNPJ ")
                .append(reservation.guest().companyCnpj())
                .append(")\n");
        text.append("Hóspede: ").append(reservation.guest().name()).append('\n');
        text.append("Quarto: ")
                .append(room.number())
                .append(" (")
                .append(room.type())
                .append(")\n");
        text.append("Entrada: ")
                .append(TextFormat.date(reservation.checkIn()))
                .append(" | Saída: ")
                .append(TextFormat.date(reservation.checkOut()))
                .append(" | ")
                .append(reservation.price().nights())
                .append(reservation.price().nights() == 1 ? " diária" : " diárias")
                .append('\n');
        text.append("Diárias: ").append(TextFormat.money(reservation.price().subtotal())).append('\n');
        text.append("Total: ").append(TextFormat.money(reservation.price().total())).append('\n');
        text.append("ISS retido (5%): ").append(TextFormat.money(iss)).append('\n');
        return text.toString();
    }
}
