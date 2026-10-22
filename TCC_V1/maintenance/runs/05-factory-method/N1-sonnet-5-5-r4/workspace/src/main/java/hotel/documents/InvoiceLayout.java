package hotel.documents;

import hotel.reservations.Identification;
import hotel.reservations.PriceCalculator;
import hotel.reservations.Reservation;
import java.math.BigDecimal;

/** The lodging invoice of a company, which ends with the ISS it withholds. */
public final class InvoiceLayout extends PortugueseLayout {

    private static final BigDecimal WITHHELD_ISS = new BigDecimal("0.05");

    @Override
    public String title() {
        return "Fatura de hospedagem";
    }

    @Override
    protected String guestLines(Reservation reservation) {
        Identification company = reservation.guest().identification();
        return "Empresa: " + company.companyName() + " (CNPJ " + company.companyCnpj() + ")\n"
                + "Hóspede: " + reservation.guest().name() + "\n";
    }

    @Override
    protected String closingLines(Reservation reservation) {
        BigDecimal iss = PriceCalculator.cents(reservation.price().total().multiply(WITHHELD_ISS));
        return "ISS retido (5%): " + money(iss) + "\n";
    }
}
