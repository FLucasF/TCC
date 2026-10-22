package hotel.documents;

import hotel.reservations.Guest;
import hotel.reservations.Price;
import hotel.reservations.PriceCalculator;
import java.math.BigDecimal;

/** The invoice of a company that books a room for an employee: the receipt naming the company, plus the ISS. */
final class InvoiceLayout extends ReceiptLayout {

    private static final BigDecimal ISS_RATE = new BigDecimal("0.05");

    @Override
    String title() {
        return "Fatura de hospedagem";
    }

    @Override
    String party(Guest guest) {
        return "Empresa: " + guest.identification().companyName()
                + " (CNPJ " + guest.identification().companyCnpj() + ")\n"
                + "Hóspede: " + guest.name() + "\n";
    }

    @Override
    String closing(Price price) {
        return "ISS retido (5%): " + money(PriceCalculator.cents(price.total().multiply(ISS_RATE))) + "\n";
    }
}
