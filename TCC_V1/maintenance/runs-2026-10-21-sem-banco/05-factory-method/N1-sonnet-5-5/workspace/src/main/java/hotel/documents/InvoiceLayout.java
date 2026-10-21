package hotel.documents;

import hotel.reservations.PriceCalculator;
import hotel.reservations.Reservation;
import java.math.BigDecimal;
import java.util.List;

/** The lodging invoice for a company: the receipt with the company instead of the CPF, and the withheld ISS. */
class InvoiceLayout extends ReceiptLayout {

    private static final BigDecimal ISS_RATE = new BigDecimal("0.05");

    @Override
    String title() {
        return "Fatura de hospedagem";
    }

    @Override
    List<String> identityLines(Reservation reservation) {
        return List.of(
                "Empresa: " + reservation.guest().identification().companyName()
                        + " (CNPJ " + reservation.guest().identification().cnpj() + ")",
                "Hóspede: " + reservation.guest().name());
    }

    @Override
    List<String> closingLines(Reservation reservation) {
        BigDecimal iss = PriceCalculator.cents(reservation.price().total().multiply(ISS_RATE));
        return List.of("ISS retido (5%): " + money(iss));
    }
}
