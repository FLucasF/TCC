package com.pousada.document;

import com.pousada.domain.PassportIdentification;
import com.pousada.domain.Reservation;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class ReceiptGenerator implements DocumentGenerator {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public String generate(Reservation reservation) {
        StringBuilder sb = new StringBuilder();
        sb.append("Booking receipt no. ").append(reservation.getId()).append("\n");

        PassportIdentification passport = (PassportIdentification) reservation.getIdentification();
        sb.append("Guest: ").append(reservation.getGuestName())
          .append(" (passport ").append(passport.passport())
          .append(", ").append(passport.country()).append(")\n");

        sb.append("Room: ").append(reservation.getRoomNumber())
          .append(" (").append(reservation.getRoomType()).append(")\n");

        sb.append("Check-in: ").append(reservation.getCheckIn().format(DATE_FORMAT))
          .append(" | Check-out: ").append(reservation.getCheckOut().format(DATE_FORMAT))
          .append(" | ").append(reservation.getNights())
          .append(reservation.getNights() == 1 ? " night" : " nights").append("\n");

        sb.append("Nights: ").append(formatMoney(reservation.getSubtotal())).append("\n");

        if (reservation.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Discount: ").append(formatMoney(reservation.getDiscount())).append("\n");
        }

        sb.append("Total: ").append(formatMoney(reservation.getTotal()));

        return sb.toString();
    }

    private String formatMoney(BigDecimal amount) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator(',');
        symbols.setDecimalSeparator('.');
        DecimalFormat df = new DecimalFormat("BRL #,##0.00", symbols);
        return df.format(amount);
    }
}
