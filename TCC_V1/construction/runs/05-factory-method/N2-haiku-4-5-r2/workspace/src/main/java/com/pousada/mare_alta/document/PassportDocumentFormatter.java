package com.pousada.mare_alta.document;

import com.pousada.mare_alta.model.Document;
import com.pousada.mare_alta.util.NumberFormatter;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class PassportDocumentFormatter implements DocumentFormatter {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public Document format(DocumentData data) {
        String nightsText = data.getNights() == 1 ? "1 night" : data.getNights() + " nights";

        StringBuilder text = new StringBuilder();
        text.append("Booking receipt no. ").append(data.getReservationId()).append("\n");
        text.append("Guest: ").append(data.getGuestName()).append(" (passport ").append(data.getGuestPassport()).append(", ").append(data.getGuestCountry()).append(")\n");
        text.append("Room: ").append(data.getRoomNumber()).append(" (").append(data.getRoomType()).append(")\n");
        text.append("Check-in: ").append(data.getCheckIn().format(DATE_FORMATTER));
        text.append(" | Check-out: ").append(data.getCheckOut().format(DATE_FORMATTER));
        text.append(" | ").append(nightsText).append("\n");
        text.append("Nights: ").append(formatCurrencyBrl(data.getSubtotal())).append("\n");

        if (data.getDiscount().signum() > 0) {
            text.append("Discount: ").append(formatCurrencyBrl(data.getDiscount())).append("\n");
        }

        text.append("Total: ").append(formatCurrencyBrl(data.getTotal())).append("\n");

        return new Document(
                "Booking receipt",
                data.getReservationId(), data.getGuestName(),
                null, null, null, data.getGuestPassport(), data.getGuestCountry(),
                data.getRoomNumber(), data.getRoomType(),
                data.getCheckIn(), data.getCheckOut(), data.getNights(),
                data.getSubtotal(), data.getDiscount(), data.getTotal(),
                text.toString().trim());
    }

    private String formatCurrencyBrl(BigDecimal value) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        DecimalFormat df = new DecimalFormat("#,##0.00", symbols);
        return "BRL " + df.format(value);
    }
}
