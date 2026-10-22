package hotel.documents;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Brazilian Portuguese: R$ 1.234,56 and 10/03/2027. */
final class PortugueseWording implements Wording {

    private static final Locale BRAZIL = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter BRAZILIAN_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public String money(BigDecimal amount) {
        return "R$ " + String.format(BRAZIL, "%,.2f", amount);
    }

    @Override
    public String date(LocalDate date) {
        return BRAZILIAN_DATE.format(date);
    }

    @Override
    public String numberSign() {
        return "nº";
    }

    @Override
    public String guest() {
        return "Hóspede";
    }

    @Override
    public String room() {
        return "Quarto";
    }

    @Override
    public String checkIn() {
        return "Entrada";
    }

    @Override
    public String checkOut() {
        return "Saída";
    }

    @Override
    public String nights(int nights) {
        return nights + (nights == 1 ? " diária" : " diárias");
    }

    @Override
    public String subtotal() {
        return "Diárias";
    }

    @Override
    public String discount() {
        return "Desconto";
    }

    @Override
    public String total() {
        return "Total";
    }
}
