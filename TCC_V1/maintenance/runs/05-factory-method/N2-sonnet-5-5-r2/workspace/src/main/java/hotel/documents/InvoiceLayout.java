package hotel.documents;

import hotel.reservations.IdentificationFields;
import hotel.reservations.Price;
import hotel.reservations.PriceCalculator;
import hotel.reservations.Reservation;
import java.math.BigDecimal;

/** The lodging invoice for a company: the receipt with the company in place of the CPF, and the withheld ISS. */
public final class InvoiceLayout extends ReceiptLayout {

    private static final BigDecimal ISS_RATE = new BigDecimal("0.05");

    @Override
    protected String title() {
        return "Fatura de hospedagem";
    }

    @Override
    protected String identityLines(Reservation reservation) {
        IdentificationFields company = reservation.guest().identification().fields();
        return "Empresa: " + company.companyName() + " (CNPJ " + company.companyCnpj() + ")\n"
                + "Hóspede: " + reservation.guest().name() + "\n";
    }

    @Override
    protected String closingLines(Price price) {
        BigDecimal iss = PriceCalculator.cents(price.total().multiply(ISS_RATE));
        return "ISS retido (5%): " + TextFormat.money(iss) + "\n";
    }
}
