package br.com.pousada.util;

import java.time.LocalDate;

public class SeasonUtil {
    public static boolean isHighSeason(LocalDate date) {
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();

        // Julho inteiro
        if (month == 7) {
            return true;
        }

        // 15 de dezembro até fim de fevereiro
        if (month == 12 && day >= 15) {
            return true;
        }

        if (month == 1 || month == 2) {
            return true;
        }

        return false;
    }
}
