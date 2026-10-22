package hotel.documents;

import hotel.reservations.Guest;
import hotel.reservations.PriceCalculator;
import hotel.reservations.Reservation;
import java.math.BigDecimal;

/** The lodging invoice of a company, with the ISS withheld at the end. */
public final class InvoiceLayout extends DocumentLayout {

    private static final BigDecimal ISS_RATE = new BigDecimal("0.05");

    public InvoiceLayout() {
        super("Fatura de hospedagem", Wording.PORTUGUESE);
    }

    @Override
    String identification(Guest guest) {
        return "Empresa: " + guest.identification().companyName()
                + " (CNPJ " + guest.identification().companyCnpj() + ")\n"
                + "Hóspede: " + guest.name() + "\n";
    }

    @Override
    String afterTotal(Reservation reservation) {
        BigDecimal iss = PriceCalculator.cents(reservation.price().total().multiply(ISS_RATE));
        return "ISS retido (5%): " + wording().money().apply(iss) + "\n";
    }
}
