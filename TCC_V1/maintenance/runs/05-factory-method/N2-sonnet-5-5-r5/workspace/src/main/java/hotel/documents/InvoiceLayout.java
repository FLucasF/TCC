package hotel.documents;

import hotel.reservations.Identification;
import hotel.reservations.Price;
import hotel.reservations.PriceCalculator;
import hotel.reservations.Reservation;
import java.math.BigDecimal;
import java.util.List;

/** The lodging invoice for a company, with the ISS withheld at the end. */
public final class InvoiceLayout extends DocumentLayout {

    private static final BigDecimal ISS_RATE = new BigDecimal("0.05");

    public InvoiceLayout() {
        super(new PortugueseWording());
    }

    @Override
    String title() {
        return "Fatura de hospedagem";
    }

    @Override
    List<String> partyLines(Reservation reservation) {
        Identification company = reservation.guest().identification();
        return List.of(
                "Empresa: " + company.companyName() + " (CNPJ " + company.companyCnpj() + ")",
                wording().guest() + ": " + reservation.guest().name());
    }

    @Override
    List<String> closingLines(Price price) {
        BigDecimal iss = PriceCalculator.cents(price.total().multiply(ISS_RATE));
        return List.of("ISS retido (5%): " + wording().money(iss));
    }
}
