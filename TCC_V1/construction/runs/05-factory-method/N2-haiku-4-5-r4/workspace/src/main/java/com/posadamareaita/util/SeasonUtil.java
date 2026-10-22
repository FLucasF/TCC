package com.posadamareaita.util;

import java.time.LocalDate;
import java.time.Month;

public class SeasonUtil {
    public static boolean isHighSeason(LocalDate date) {
        Month month = date.getMonth();
        int dayOfMonth = date.getDayOfMonth();

        // Julho inteiro
        if (month == Month.JULY) {
            return true;
        }

        // 15 de dezembro até 28 de fevereiro
        if (month == Month.DECEMBER && dayOfMonth >= 15) {
            return true;
        }
        if (month == Month.JANUARY) {
            return true;
        }
        if (month == Month.FEBRUARY && dayOfMonth <= 28) {
            return true;
        }

        return false;
    }
}
