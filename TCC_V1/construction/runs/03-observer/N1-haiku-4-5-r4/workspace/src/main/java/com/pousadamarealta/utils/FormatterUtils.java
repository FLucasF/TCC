package com.pousadamarealta.utils;

import com.pousadamarealta.models.Reservation;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class FormatterUtils {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter CSV_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
    private static final NumberFormat CSV_NUMBER_FORMAT = NumberFormat.getInstance(Locale.US);

    static {
        CURRENCY_FORMAT.setMinimumFractionDigits(2);
        CURRENCY_FORMAT.setMaximumFractionDigits(2);
        CSV_NUMBER_FORMAT.setMinimumFractionDigits(2);
        CSV_NUMBER_FORMAT.setMaximumFractionDigits(2);
    }

    public static String formatDate(LocalDate date) {
        return date.format(DATE_FORMATTER);
    }

    public static String formatCsvDate(LocalDate date) {
        return date.format(CSV_DATE_FORMATTER);
    }

    public static String formatCurrency(BigDecimal value) {
        return CURRENCY_FORMAT.format(value);
    }

    public static String formatCsvNumber(BigDecimal value) {
        return CSV_NUMBER_FORMAT.format(value);
    }

    public static String formatCpf(String cpf) {
        if (cpf == null || cpf.length() != 11) return cpf;
        return cpf.substring(0, 3) + "." +
               cpf.substring(3, 6) + "." +
               cpf.substring(6, 9) + "-" +
               cpf.substring(9, 11);
    }

    public static String generateReceipt(Reservation res) {
        StringBuilder sb = new StringBuilder();
        sb.append("Comprovante de reserva nº ").append(res.id).append("\n");
        sb.append("Hóspede: ").append(res.guestName)
            .append(" (CPF ").append(formatCpf(res.guestCpf)).append(")\n");
        sb.append("Quarto: ").append(res.roomNumber).append("\n");
        sb.append("Entrada: ").append(formatDate(res.checkIn))
            .append(" | Saída: ").append(formatDate(res.checkOut))
            .append(" | ").append(res.nights).append(" diária").append(res.nights != 1 ? "s" : "").append("\n");
        sb.append("Diárias: ").append(formatCurrency(res.subtotal)).append("\n");

        if (res.discount.compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Desconto: ").append(formatCurrency(res.discount)).append("\n");
        }

        sb.append("Total: ").append(formatCurrency(res.total));
        return sb.toString();
    }
}
