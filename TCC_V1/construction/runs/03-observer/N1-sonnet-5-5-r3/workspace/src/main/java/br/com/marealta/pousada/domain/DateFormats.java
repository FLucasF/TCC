package br.com.marealta.pousada.domain;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public final class DateFormats {

    private static final DateTimeFormatter BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private DateFormats() {
    }

    /** Formato do comprovante: 10/03/2027. */
    public static String format(LocalDate date) {
        return BR.format(date);
    }
}
