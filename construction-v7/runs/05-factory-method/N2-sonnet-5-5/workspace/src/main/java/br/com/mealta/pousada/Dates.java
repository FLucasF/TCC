package br.com.mealta.pousada;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

final class Dates {
    private static final DateTimeFormatter BR = DateTimeFormatter.ofPattern("dd/MM/uuuu");

    private Dates() {}

    static String br(LocalDate date) {
        return BR.format(date);
    }
}
