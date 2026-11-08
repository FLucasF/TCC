package com.marealta;

import java.time.LocalDate;

public final class Seasons {
    private Seasons() {}

    /** Alta temporada: de 15/12 ao fim de fevereiro, e julho inteiro. */
    public static boolean isHigh(LocalDate night) {
        int month = night.getMonthValue();
        return month == 1 || month == 2 || month == 7 || (month == 12 && night.getDayOfMonth() >= 15);
    }
}
