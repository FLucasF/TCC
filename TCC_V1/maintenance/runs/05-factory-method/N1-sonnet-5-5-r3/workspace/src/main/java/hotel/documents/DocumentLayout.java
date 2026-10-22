package hotel.documents;

import hotel.reservations.Identification;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Locale;

/**
 * How a kind of document is written. The sequence of lines is the same for all of them and is written once,
 * in {@link #text}; each kind says only what is its own.
 */
abstract class DocumentLayout {

    /** The layout of the document that goes with this way of identifying the guest. */
    static DocumentLayout of(Identification identification) {
        return switch (identification) {
            case Identification.Cpf cpf -> new ReceiptLayout();
            case Identification.Company company -> new InvoiceLayout();
            case Identification.Passport passport -> new BookingReceiptLayout();
        };
    }

    abstract String title();

    /** What goes after the title: "nº" or "no.". */
    abstract String numberSign();

    abstract String guestLine(ReservationDocument document);

    abstract String roomLabel();

    abstract String stayLine(ReservationDocument document);

    abstract String subtotalLabel();

    abstract String discountLabel();

    abstract String totalLabel();

    abstract String money(BigDecimal amount);

    /** Lines that come after the total; none unless the kind says otherwise. */
    String closing(ReservationDocument document) {
        return "";
    }

    final String text(ReservationDocument document) {
        StringBuilder text = new StringBuilder();
        text.append(title()).append(' ').append(numberSign()).append(' ').append(document.reservationId()).append('\n');
        text.append(guestLine(document));
        text.append(roomLabel()).append(": ").append(document.roomNumber())
                .append(" (").append(document.roomType()).append(")\n");
        text.append(stayLine(document)).append('\n');
        text.append(subtotalLabel()).append(": ").append(money(document.subtotal())).append('\n');
        if (document.discount().signum() > 0) {
            text.append(discountLabel()).append(": ").append(money(document.discount())).append('\n');
        }
        text.append(totalLabel()).append(": ").append(money(document.total())).append('\n');
        text.append(closing(document));
        return text.toString();
    }

    /** The receipt in Portuguese, for the guest identified by CPF. */
    static class ReceiptLayout extends DocumentLayout {
        @Override
        String title() {
            return "Comprovante de reserva";
        }

        @Override
        String numberSign() {
            return "nº";
        }

        @Override
        String guestLine(ReservationDocument document) {
            return "Hóspede: " + document.guestName() + " (CPF " + document.guestCpf() + ")\n";
        }

        @Override
        String roomLabel() {
            return "Quarto";
        }

        @Override
        String stayLine(ReservationDocument document) {
            return "Entrada: " + TextFormat.date(document.checkIn())
                    + " | Saída: " + TextFormat.date(document.checkOut())
                    + " | " + document.nights() + (document.nights() == 1 ? " diária" : " diárias");
        }

        @Override
        String subtotalLabel() {
            return "Diárias";
        }

        @Override
        String discountLabel() {
            return "Desconto";
        }

        @Override
        String totalLabel() {
            return "Total";
        }

        @Override
        String money(BigDecimal amount) {
            return TextFormat.money(amount);
        }
    }

    /** The invoice for the company: the receipt, with the company instead of the CPF and the ISS at the end. */
    static class InvoiceLayout extends ReceiptLayout {
        private static final BigDecimal WITHHELD_ISS = new BigDecimal("0.05");

        @Override
        String title() {
            return "Fatura de hospedagem";
        }

        @Override
        String guestLine(ReservationDocument document) {
            return "Empresa: " + document.companyName() + " (CNPJ " + document.companyCnpj() + ")\n"
                    + "Hóspede: " + document.guestName() + "\n";
        }

        @Override
        String closing(ReservationDocument document) {
            BigDecimal iss = document.total().multiply(WITHHELD_ISS).setScale(2, RoundingMode.HALF_EVEN);
            return "ISS retido (5%): " + money(iss) + "\n";
        }
    }

    /** The receipt in English, for the foreign guest identified by passport. */
    static class BookingReceiptLayout extends DocumentLayout {
        @Override
        String title() {
            return "Booking receipt";
        }

        @Override
        String numberSign() {
            return "no.";
        }

        @Override
        String guestLine(ReservationDocument document) {
            return "Guest: " + document.guestName() + " (passport " + document.guestPassport() + ", "
                    + document.guestCountry() + ")\n";
        }

        @Override
        String roomLabel() {
            return "Room";
        }

        @Override
        String stayLine(ReservationDocument document) {
            return "Check-in: " + date(document.checkIn()) + " | Check-out: " + date(document.checkOut())
                    + " | " + document.nights() + (document.nights() == 1 ? " night" : " nights");
        }

        @Override
        String subtotalLabel() {
            return "Nights";
        }

        @Override
        String discountLabel() {
            return "Discount";
        }

        @Override
        String totalLabel() {
            return "Total";
        }

        @Override
        String money(BigDecimal amount) {
            return "BRL " + String.format(Locale.US, "%,.2f", amount);
        }

        private static String date(LocalDate date) {
            return date.toString();
        }
    }
}
