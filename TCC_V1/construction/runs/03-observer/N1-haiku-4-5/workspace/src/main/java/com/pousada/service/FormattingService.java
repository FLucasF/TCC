package com.pousada.service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class FormattingService {
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
  private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

  public static String formatDate(LocalDate date) {
    return date.format(DATE_FORMATTER);
  }

  public static String formatCurrency(BigDecimal amount) {
    return "R$ " + String.format("%.2f", amount).replace(".", ",");
  }

  public static String formatCsv(BigDecimal amount) {
    return String.format("%.2f", amount);
  }

  public static String formatCsvDate(LocalDate date) {
    return date.toString();
  }
}
