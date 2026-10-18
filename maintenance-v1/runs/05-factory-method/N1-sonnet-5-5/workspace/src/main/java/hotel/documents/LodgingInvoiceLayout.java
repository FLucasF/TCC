package hotel.documents;

import hotel.reservations.Guest;
import hotel.reservations.Reservation;
import java.math.BigDecimal;
import java.math.RoundingMode;

/** The lodging invoice, for companies: shows the company instead of the CPF and ends with the withheld ISS. */
class LodgingInvoiceLayout extends PortugueseReceiptLayout {

    private static final BigDecimal ISS_RATE = new BigDecimal("0.05");

    @Override
    public String title() {
        return "Fatura de hospedagem";
    }

    @Override
    String whoLines(Reservation reservation) {
        Guest guest = reservation.guest();
        return "Empresa: " + guest.companyName() + " (CNPJ " + guest.companyCnpj() + ")\n"
                + "Hóspede: " + guest.name() + "\n";
    }

    @Override
    String closingLines(Reservation reservation) {
        BigDecimal iss = reservation.price().total().multiply(ISS_RATE).setScale(2, RoundingMode.HALF_EVEN);
        return "ISS retido (5%): " + TextFormat.money(iss) + "\n";
    }
}
