package hotel.documents;

import java.math.BigDecimal;
import java.time.LocalDate;

/** The documents written in Portuguese: the words, the money and the dates are the same in all of them. */
abstract class PortugueseLayout extends DocumentLayout {

    @Override
    protected String headline(long reservationId) {
        return title() + " nº " + reservationId;
    }

    @Override
    protected String roomLabel() {
        return "Quarto";
    }

    @Override
    protected String stay(LocalDate checkIn, LocalDate checkOut, int nights) {
        return "Entrada: " + TextFormat.date(checkIn)
                + " | Saída: " + TextFormat.date(checkOut)
                + " | " + nights + (nights == 1 ? " diária" : " diárias");
    }

    @Override
    protected String subtotalLabel() {
        return "Diárias";
    }

    @Override
    protected String discountLabel() {
        return "Desconto";
    }

    @Override
    protected String totalLabel() {
        return "Total";
    }

    @Override
    protected String money(BigDecimal amount) {
        return TextFormat.money(amount);
    }
}
