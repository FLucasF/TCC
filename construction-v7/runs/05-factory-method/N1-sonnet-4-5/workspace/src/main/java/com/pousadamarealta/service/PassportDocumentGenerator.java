package com.pousadamarealta.service;

import com.pousadamarealta.domain.*;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class PassportDocumentGenerator implements DocumentGenerator {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DecimalFormat MONEY_FORMAT;

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator(',');
        symbols.setDecimalSeparator('.');
        MONEY_FORMAT = new DecimalFormat("BRL #,##0.00", symbols);
    }

    @Override
    public Document generate(Reservation reservation, Room room, BigDecimal subtotal, BigDecimal discount, BigDecimal total) {
        PassportIdentification identification = (PassportIdentification) reservation.getIdentification();

        StringBuilder text = new StringBuilder();
        text.append("Booking receipt no. ").append(reservation.getId()).append("\n");
        text.append("Guest: ").append(reservation.getGuestName())
            .append(" (passport ").append(identification.getPassport())
            .append(", ").append(identification.getCountry()).append(")\n");
        text.append("Room: ").append(room.getNumber()).append(" (").append(room.getType()).append(")\n");
        text.append("Check-in: ").append(reservation.getCheckIn().format(DATE_FORMAT))
            .append(" | Check-out: ").append(reservation.getCheckOut().format(DATE_FORMAT))
            .append(" | ").append(reservation.getNights())
            .append(reservation.getNights() == 1 ? " night" : " nights").append("\n");
        text.append("Nights: ").append(formatMoney(subtotal)).append("\n");

        if (discount.compareTo(BigDecimal.ZERO) > 0) {
            text.append("Discount: ").append(formatMoney(discount)).append("\n");
        }

        text.append("Total: ").append(formatMoney(total));

        return new Document(
            "Booking receipt",
            reservation.getId(),
            reservation.getGuestName(),
            null,
            null,
            null,
            identification.getPassport(),
            identification.getCountry(),
            room.getNumber(),
            room.getType(),
            reservation.getCheckIn(),
            reservation.getCheckOut(),
            reservation.getNights(),
            subtotal,
            discount,
            total,
            text.toString()
        );
    }

    private String formatMoney(BigDecimal amount) {
        return MONEY_FORMAT.format(amount);
    }
}
