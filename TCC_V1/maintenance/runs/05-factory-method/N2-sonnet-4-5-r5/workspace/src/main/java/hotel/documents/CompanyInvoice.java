package hotel.documents;

import hotel.reservations.Reservation;
import hotel.rooms.Room;
import java.math.BigDecimal;
import java.math.RoundingMode;

/** Invoice for a company with CNPJ. */
public final class CompanyInvoice extends ReservationDocument {

    private static final BigDecimal ISS_RATE = new BigDecimal("0.05");

    private final String companyCnpj;
    private final String companyName;
    private final BigDecimal iss;

    public CompanyInvoice(Reservation reservation, Room room) {
        super(reservation, room);
        this.companyCnpj = reservation.guest().companyCnpj();
        this.companyName = reservation.guest().companyName();
        this.iss = total.multiply(ISS_RATE).setScale(2, RoundingMode.HALF_EVEN);
    }

    public String companyCnpj() {
        return companyCnpj;
    }

    public String companyName() {
        return companyName;
    }

    public BigDecimal iss() {
        return iss;
    }

    @Override
    public String text() {
        StringBuilder text = new StringBuilder();
        text.append("Fatura de hospedagem nº ").append(reservationId).append('\n');
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
        text.append("ISS retido (5%): ").append(TextFormat.money(iss)).append('\n');
        return text.toString();
    }
}
