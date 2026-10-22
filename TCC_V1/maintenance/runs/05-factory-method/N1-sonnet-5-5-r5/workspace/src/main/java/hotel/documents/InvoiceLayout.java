package hotel.documents;

import hotel.reservations.Identification;
import hotel.reservations.PriceCalculator;
import hotel.reservations.Reservation;
import java.math.BigDecimal;

/** The lodging invoice of a company that books a room for its employee; it shows the ISS withheld. */
final class InvoiceLayout extends DocumentLayout {

    private static final BigDecimal ISS_RATE = new BigDecimal("0.05");

    InvoiceLayout() {
        super("Fatura de hospedagem", DocumentLanguage.PORTUGUESE);
    }

    @Override
    String guestLines(Reservation reservation) {
        Identification company = reservation.guest().identification();
        return "Empresa: " + company.companyName() + " (CNPJ " + company.companyCnpj() + ")\n"
                + guestName(reservation) + "\n";
    }

    @Override
    String closingLines(Reservation reservation) {
        BigDecimal iss = PriceCalculator.cents(reservation.price().total().multiply(ISS_RATE));
        return "ISS retido (5%): " + money(iss) + "\n";
    }
}
