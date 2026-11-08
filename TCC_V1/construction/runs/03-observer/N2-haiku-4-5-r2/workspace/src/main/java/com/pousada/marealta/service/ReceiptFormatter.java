package com.pousada.marealta.service;

import com.pousada.marealta.domain.Reservation;
import com.pousada.marealta.repository.RoomRepository;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class ReceiptFormatter {
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
  private static final DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
  static {
    symbols.setDecimalSeparator(',');
    symbols.setGroupingSeparator('.');
  }
  private static final DecimalFormat MONEY_FORMATTER = new DecimalFormat("0.00", symbols);

  public static String formatReceipt(Reservation reservation) throws SQLException {
    var room = RoomRepository.findByNumber(reservation.getRoomNumber());
    if (room == null) {
      throw new SQLException("Room not found: " + reservation.getRoomNumber());
    }

    String nightsLabel = reservation.getNights() == 1 ? "1 diária" : reservation.getNights() + " diárias";
    String checkInStr = reservation.getCheckIn().format(DATE_FORMATTER);
    String checkOutStr = reservation.getCheckOut().format(DATE_FORMATTER);
    String cpfFormatted = formatCpf(reservation.getGuestCpf());
    String subtotalStr = formatMoney(reservation.getSubtotal());
    String totalStr = formatMoney(reservation.getTotal());

    StringBuilder sb = new StringBuilder();
    sb.append("Comprovante de reserva nº ").append(reservation.getId()).append("\n");
    sb.append("Hóspede: ").append(reservation.getGuestName()).append(" (CPF ").append(cpfFormatted).append(")\n");
    sb.append("Quarto: ").append(reservation.getRoomNumber()).append(" (").append(room.getType()).append(")\n");
    sb.append("Entrada: ").append(checkInStr).append(" | Saída: ").append(checkOutStr).append(" | ").append(nightsLabel).append("\n");
    sb.append("Diárias: ").append(subtotalStr).append("\n");

    if (reservation.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
      String discountStr = formatMoney(reservation.getDiscount());
      sb.append("Desconto: ").append(discountStr).append("\n");
    }

    sb.append("Total: ").append(totalStr);
    return sb.toString();
  }

  private static String formatCpf(String cpf) {
    if (cpf.length() != 11) {
      return cpf;
    }
    return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." +
        cpf.substring(6, 9) + "-" + cpf.substring(9);
  }

  public static String formatMoney(BigDecimal value) {
    return "R$ " + MONEY_FORMATTER.format(value);
  }

  public static String formatDate(java.time.LocalDate date) {
    return date.format(DATE_FORMATTER);
  }
}
