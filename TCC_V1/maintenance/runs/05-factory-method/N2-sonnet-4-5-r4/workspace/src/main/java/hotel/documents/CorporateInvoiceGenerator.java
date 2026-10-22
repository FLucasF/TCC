package hotel.documents;

import hotel.reservations.CorporateIdentity;
import hotel.reservations.Reservation;
import hotel.rooms.Room;
import java.math.BigDecimal;
import java.math.RoundingMode;

final class CorporateInvoiceGenerator implements DocumentGenerator {

    @Override
    public ReservationReceipt generate(Reservation reservation, Room room) {
        CorporateIdentity identity = (CorporateIdentity) reservation.guest().identity();
        String text = buildText(reservation, room, identity);
        return new ReservationReceipt(
                "Fatura de hospedagem",
                reservation.id(),
                reservation.guest().name(),
                null,
                identity.cnpj(),
                identity.companyName(),
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

    private String buildText(Reservation reservation, Room room, CorporateIdentity identity) {
        BigDecimal issRetained = reservation.price().total()
                .multiply(new BigDecimal("0.05"))
                .setScale(2, RoundingMode.HALF_EVEN);

        StringBuilder text = new StringBuilder();
        text.append("Fatura de hospedagem nº ").append(reservation.id()).append('\n');
        text.append("Empresa: ").append(identity.companyName())
                .append(" (CNPJ ").append(identity.cnpj()).append(")\n");
        text.append("Hóspede: ").append(reservation.guest().name()).append('\n');
        text.append("Quarto: ").append(room.number()).append(" (").append(room.type()).append(")\n");
        text.append("Entrada: ").append(TextFormat.date(reservation.checkIn()))
                .append(" | Saída: ").append(TextFormat.date(reservation.checkOut()))
                .append(" | ").append(reservation.price().nights())
                .append(reservation.price().nights() == 1 ? " diária" : " diárias").append('\n');
        text.append("Diárias: ").append(TextFormat.money(reservation.price().subtotal())).append('\n');
        if (reservation.price().discount().signum() > 0) {
            text.append("Desconto: ").append(TextFormat.money(reservation.price().discount())).append('\n');
        }
        text.append("Total: ").append(TextFormat.money(reservation.price().total())).append('\n');
        text.append("ISS retido (5%): ").append(TextFormat.money(issRetained)).append('\n');
        return text.toString();
    }
}
