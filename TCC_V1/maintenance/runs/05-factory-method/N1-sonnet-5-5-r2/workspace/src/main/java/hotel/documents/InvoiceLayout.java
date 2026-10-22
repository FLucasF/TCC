package hotel.documents;

import hotel.reservations.Identification;
import hotel.reservations.PriceCalculator;
import hotel.reservations.Reservation;
import java.math.BigDecimal;
import java.util.List;

/** The invoice of a company that books a room for someone who works for it. */
final class InvoiceLayout extends DocumentLayout {

    private static final BigDecimal WITHHELD_ISS = new BigDecimal("0.05");

    @Override
    String title() {
        return "Fatura de hospedagem";
    }

    @Override
    Language language() {
        return Language.PORTUGUESE;
    }

    @Override
    List<String> identityLines(Reservation reservation) {
        Identification company = reservation.guest().identification();
        return List.of(
                "Empresa: " + company.companyName() + " (CNPJ " + company.companyCnpj() + ")",
                "Hóspede: " + reservation.guest().name());
    }

    @Override
    List<String> closingLines(Reservation reservation) {
        BigDecimal iss = PriceCalculator.cents(reservation.price().total().multiply(WITHHELD_ISS));
        return List.of("ISS retido (5%): " + language().money(iss));
    }
}
