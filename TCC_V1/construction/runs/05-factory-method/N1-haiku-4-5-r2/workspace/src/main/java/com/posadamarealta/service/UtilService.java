package com.posadamarealta.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

public class UtilService {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_FORMAT_ISO = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DATE_FORMAT_ENGLISH = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static boolean isHighSeason(LocalDate date) {
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();

        return (month == 7) || (month == 12) || (month == 1) || (month == 2);
    }

    public static BigDecimal roundHalfEven(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal calculateNightlyRate(BigDecimal baseRate, LocalDate date) {
        if (isHighSeason(date)) {
            return roundHalfEven(baseRate.multiply(new BigDecimal("1.20")));
        }
        return baseRate;
    }

    public static int calculateNights(LocalDate checkIn, LocalDate checkOut) {
        return (int) java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
    }

    public static String formatDate(LocalDate date) {
        return date.format(DATE_FORMAT);
    }

    public static String formatDateISO(LocalDate date) {
        return date.format(DATE_FORMAT_ISO);
    }

    public static String formatDateEnglish(LocalDate date) {
        return date.format(DATE_FORMAT_ENGLISH);
    }

    public static String formatMoney(BigDecimal value) {
        String parts[] = String.format("%.2f", value).split("\\.");
        String integer = parts[0];
        String decimal = parts[1];

        StringBuilder sb = new StringBuilder("R$ ");
        int len = integer.length();
        for (int i = 0; i < len; i++) {
            if (i > 0 && (len - i) % 3 == 0) {
                sb.append(".");
            }
            sb.append(integer.charAt(i));
        }
        sb.append(",").append(decimal);
        return sb.toString();
    }

    public static String formatMoneyEnglish(BigDecimal value) {
        String parts[] = String.format("%.2f", value).split("\\.");
        String integer = parts[0];
        String decimal = parts[1];

        StringBuilder sb = new StringBuilder("BRL ");
        int len = integer.length();
        for (int i = 0; i < len; i++) {
            if (i > 0 && (len - i) % 3 == 0) {
                sb.append(",");
            }
            sb.append(integer.charAt(i));
        }
        sb.append(".").append(decimal);
        return sb.toString();
    }

    public static String formatCSVMoney(BigDecimal value) {
        return String.format("%.2f", value);
    }

    public static YearMonth parseYearMonth(String yearMonth) {
        return YearMonth.parse(yearMonth, DateTimeFormatter.ofPattern("yyyy-MM"));
    }

    public static LocalDate parseDate(String date) {
        return LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE);
    }
}
