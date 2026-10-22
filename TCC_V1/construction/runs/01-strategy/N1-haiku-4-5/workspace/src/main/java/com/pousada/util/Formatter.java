package com.pousada.util;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class Formatter {
    private static final DateTimeFormatter DATE_DISPLAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter MONTH_ISO = DateTimeFormatter.ofPattern("yyyy-MM");

    public static String formatDateDisplay(LocalDate date) {
        return date.format(DATE_DISPLAY);
    }

    public static String formatDateIso(LocalDate date) {
        return date.format(DATE_ISO);
    }

    public static String formatMonthIso(LocalDate date) {
        return date.format(MONTH_ISO);
    }

    public static String formatCurrency(BigDecimal value) {
        return String.format("R$ %,.2f", value).replace(",", ".");
    }

    public static String formatCurrencyForCsv(BigDecimal value) {
        return String.format("%.2f", value);
    }

    public static String formatCpf(String cpf) {
        if (cpf == null || cpf.length() != 11) {
            return cpf;
        }
        return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." +
               cpf.substring(6, 9) + "-" + cpf.substring(9, 11);
    }

    public static String formatNights(int nights) {
        if (nights == 1) {
            return "1 diária";
        }
        return nights + " diárias";
    }
}
