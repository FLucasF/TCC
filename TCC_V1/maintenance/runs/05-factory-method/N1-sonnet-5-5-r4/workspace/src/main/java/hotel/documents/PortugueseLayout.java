package hotel.documents;

import java.math.BigDecimal;
import java.time.LocalDate;

/** The words and formats of the documents in Brazilian Portuguese. */
abstract class PortugueseLayout extends DocumentLayout {

    private static final Words WORDS = new Words(
            "nº", "Hóspede", "Quarto", "Entrada", "Saída", "diária", "diárias", "Diárias", "Desconto", "Total");

    @Override
    protected Words words() {
        return WORDS;
    }

    @Override
    protected String money(BigDecimal amount) {
        return TextFormat.money(amount);
    }

    @Override
    protected String date(LocalDate date) {
        return TextFormat.date(date);
    }
}
