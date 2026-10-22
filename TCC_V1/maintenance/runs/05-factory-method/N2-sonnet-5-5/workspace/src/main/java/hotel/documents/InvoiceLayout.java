package hotel.documents;

import hotel.reservations.Identification;
import hotel.reservations.Price;
import hotel.reservations.PriceCalculator;
import hotel.reservations.Reservation;
import java.math.BigDecimal;

/** The lodging invoice in Portuguese, for the company that pays the stay. */
public class InvoiceLayout extends ReceiptLayout {

    static final BigDecimal WITHHELD_ISS_RATE = new BigDecimal("0.05");

    @Override
    public String title() {
        return "Fatura de hospedagem";
    }

    @Override
    protected String guestLines(Reservation reservation) {
        Identification company = reservation.guest().identification();
        return "Empresa: " + company.companyName() + " (CNPJ " + company.companyCnpj() + ")\n"
                + "Hóspede: " + reservation.guest().name();
    }

    @Override
    protected String closingLines(Price price) {
        BigDecimal iss = PriceCalculator.cents(price.total().multiply(WITHHELD_ISS_RATE));
        return "ISS retido (5%): " + TextFormat.money(iss);
    }
}
