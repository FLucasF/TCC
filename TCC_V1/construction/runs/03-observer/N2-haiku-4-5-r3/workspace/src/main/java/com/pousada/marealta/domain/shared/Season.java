package com.pousada.marealta.domain.shared;

import java.time.LocalDate;
import java.time.Month;

public enum Season {
    HIGH_SEASON(1.20),
    REGULAR(1.0);

    private final double multiplier;

    Season(double multiplier) {
        this.multiplier = multiplier;
    }

    public double multiplier() {
        return multiplier;
    }

    public static Season forDate(LocalDate date) {
        Month month = date.getMonth();
        int dayOfMonth = date.getDayOfMonth();

        if (month == Month.JULY) {
            return HIGH_SEASON;
        }

        if (month == Month.DECEMBER && dayOfMonth >= 15) {
            return HIGH_SEASON;
        }

        if ((month == Month.JANUARY || month == Month.FEBRUARY) && dayOfMonth <= 28) {
            return HIGH_SEASON;
        }

        return REGULAR;
    }
}
