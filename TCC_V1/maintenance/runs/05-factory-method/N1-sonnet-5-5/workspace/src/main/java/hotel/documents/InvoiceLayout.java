package hotel.documents;

import hotel.reservations.PriceCalculator;
import hotel.reservations.Reservation;
import java.math.BigDecimal;

/** The invoice of a company that books a room for its employee. */
final class InvoiceLayout extends DocumentLayout {

    private static final BigDecimal WITHHELD_ISS = new BigDecimal("0.05");

    InvoiceLayout() {
        super(PORTUGUESE);
    }

    @Override
    String title() {
        return "Fatura de hospedagem";
    }

    @Override
    String guestLines(Reservation reservation) {
        var identification = reservation.guest().identification();
        return "Empresa: " + identification.companyName() + " (CNPJ " + identification.companyCnpj() + ")\n"
                + "Hóspede: " + reservation.guest().name() + "\n";
    }

    @Override
    String footer(Reservation reservation) {
        BigDecimal iss = PriceCalculator.cents(reservation.price().total().multiply(WITHHELD_ISS));
        return "ISS retido (5%): " + money(iss) + "\n";
    }
}
