package pousada;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public final class Dates {
    private static final DateTimeFormatter PT = DateTimeFormatter.ofPattern("dd/MM/uuuu");

    private Dates() {}

    public static String pt(LocalDate d) {
        return PT.format(d);
    }
}
