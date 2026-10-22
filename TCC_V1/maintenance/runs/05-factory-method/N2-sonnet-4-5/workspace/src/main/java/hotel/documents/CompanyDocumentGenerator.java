package hotel.documents;

import hotel.reservations.Guest;
import hotel.reservations.Price;
import hotel.reservations.PriceCalculator;
import hotel.rooms.Room;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Generates the invoice for a company reservation (CNPJ). */
public final class CompanyDocumentGenerator implements DocumentGenerator {

    private static final BigDecimal ISS_RATE = new BigDecimal("0.05");

    @Override
    public String title() {
        return "Fatura de hospedagem";
    }

    @Override
    public String formatDocument(
            long reservationId, Guest guest, Room room, LocalDate checkIn, LocalDate checkOut, Price price) {
        BigDecimal issRetained = PriceCalculator.cents(price.total().multiply(ISS_RATE));

        StringBuilder text = new StringBuilder();
        text.append(title()).append(" nº ").append(reservationId).append('\n');
        text.append("Empresa: ")
                .append(guest.companyName())
                .append(" (CNPJ ")
                .append(guest.companyCnpj())
                .append(")\n");
        text.append("Hóspede: ").append(guest.name()).append('\n');
        text.append("Quarto: ").append(room.number()).append(" (").append(room.type()).append(")\n");
        text.append("Entrada: ").append(TextFormat.date(checkIn))
                .append(" | Saída: ").append(TextFormat.date(checkOut))
                .append(" | ").append(price.nights()).append(price.nights() == 1 ? " diária" : " diárias")
                .append('\n');
        text.append("Diárias: ").append(TextFormat.money(price.subtotal())).append('\n');
        if (price.discount().signum() > 0) {
            text.append("Desconto: ").append(TextFormat.money(price.discount())).append('\n');
        }
        text.append("Total: ").append(TextFormat.money(price.total())).append('\n');
        text.append("ISS retido (5%): ").append(TextFormat.money(issRetained)).append('\n');
        return text.toString();
    }
}
