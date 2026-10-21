package br.com.marealta.domain;

import java.time.LocalDate;
import java.time.Month;

public final class Season {
    private Season() {}

    /** Alta temporada: 15/12 até o fim de fevereiro, e julho inteiro. */
    public static boolean isHigh(LocalDate night) {
        Month m = night.getMonth();
        return m == Month.JANUARY || m == Month.FEBRUARY || m == Month.JULY
                || (m == Month.DECEMBER && night.getDayOfMonth() >= 15);
    }
}
