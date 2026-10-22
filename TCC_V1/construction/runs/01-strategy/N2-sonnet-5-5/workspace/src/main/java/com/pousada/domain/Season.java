package com.pousada.domain;

import java.time.LocalDate;

public final class Season {

    private Season() {
    }

    /** Alta temporada: de 15/12 ao fim de fevereiro e todo o mês de julho. */
    public static boolean isHigh(LocalDate night) {
        int month = night.getMonthValue();
        return month == 1 || month == 2 || month == 7 || (month == 12 && night.getDayOfMonth() >= 15);
    }
}
