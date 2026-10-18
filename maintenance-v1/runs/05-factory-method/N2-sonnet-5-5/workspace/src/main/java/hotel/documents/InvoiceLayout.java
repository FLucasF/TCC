package hotel.documents;

import hotel.reservations.PriceCalculator;
import java.math.BigDecimal;

/** The lodging invoice for a company, in Portuguese: the company instead of the CPF, and the withheld ISS. */
final class InvoiceLayout extends ReceiptLayout {

    private static final BigDecimal ISS_RATE = new BigDecimal("0.05");

    @Override
    public String title() {
        return "Fatura de hospedagem";
    }

    @Override
    String identification(ReservationDocument document) {
        return "Empresa: " + document.companyName() + " (CNPJ " + document.companyCnpj() + ")\n"
                + "Hóspede: " + document.guestName() + "\n";
    }

    @Override
    String closing(ReservationDocument document) {
        BigDecimal iss = PriceCalculator.cents(document.total().multiply(ISS_RATE));
        return "ISS retido (5%): " + TextFormat.money(iss) + "\n";
    }
}
