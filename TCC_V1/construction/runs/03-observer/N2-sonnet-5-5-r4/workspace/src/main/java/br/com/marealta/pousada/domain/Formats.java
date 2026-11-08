package br.com.marealta.pousada.domain;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public final class Formats {

    private static final DateTimeFormatter BR_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private Formats() {
    }

    /** Escreve como 10/03/2027. */
    public static String date(LocalDate date) {
        return BR_DATE.format(date);
    }
}
