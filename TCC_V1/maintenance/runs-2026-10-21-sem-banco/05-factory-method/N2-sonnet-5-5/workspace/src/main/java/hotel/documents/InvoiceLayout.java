package hotel.documents;

import hotel.reservations.Guest;
import hotel.reservations.PriceCalculator;
import java.math.BigDecimal;

/** The lodging invoice of a company, which withholds ISS on the total. */
public final class InvoiceLayout extends PortugueseLayout {

    private static final BigDecimal ISS_RATE = new BigDecimal("0.05");

    @Override
    protected String title() {
        return "Fatura de hospedagem";
    }

    @Override
    protected String identification(Guest guest) {
        return "Empresa: " + guest.identification().companyName()
                + " (CNPJ " + guest.identification().companyCnpj() + ")\n"
                + "Hóspede: " + guest.name() + "\n";
    }

    @Override
    protected String closing(BigDecimal total) {
        return "ISS retido (5%): " + money(PriceCalculator.cents(total.multiply(ISS_RATE))) + "\n";
    }
}
