package br.com.mare.domain;

import java.time.LocalDate;
import java.time.Month;

/** Alta temporada: de 15/12 ao fim de fevereiro e todo o mês de julho. */
public final class HighSeason {
    private HighSeason() {}

    public static boolean includes(LocalDate date) {
        Month month = date.getMonth();
        return month == Month.JANUARY || month == Month.FEBRUARY || month == Month.JULY
                || (month == Month.DECEMBER && date.getDayOfMonth() >= 15);
    }
}
