package com.pousadamarealta.document;

import com.pousadamarealta.domain.GuestIdentification;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.RoomType;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

final class ForeignGuestDocumentGenerator extends DocumentGenerator {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DecimalFormat CURRENCY_FORMATTER;

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator(',');
        symbols.setDecimalSeparator('.');
        CURRENCY_FORMATTER = new DecimalFormat("BRL #,##0.00", symbols);
    }

    @Override
    protected String formatTitle(long reservationId) {
        return "Booking receipt no. " + reservationId;
    }

    @Override
    protected String formatGuestIdentification(Reservation reservation) {
        GuestIdentification.ForeignGuest foreign =
            (GuestIdentification.ForeignGuest) reservation.guestIdentification();
        return "Guest: " + reservation.guestName() +
               " (passport " + foreign.passport() + ", " + foreign.country() + ")";
    }

    @Override
    protected String formatRoom(int roomNumber, RoomType roomType) {
        return "Room: " + roomNumber + " (" + roomType + ")";
    }

    @Override
    protected String formatPeriod(LocalDate checkIn, LocalDate checkOut, int nights) {
        String nightsText = nights == 1 ? "1 night" : nights + " nights";
        return "Check-in: " + checkIn.format(DATE_FORMATTER) +
               " | Check-out: " + checkOut.format(DATE_FORMATTER) +
               " | " + nightsText;
    }

    @Override
    protected String formatSubtotal(BigDecimal subtotal) {
        return "Nights: " + CURRENCY_FORMATTER.format(subtotal);
    }

    @Override
    protected String formatDiscount(BigDecimal discount) {
        return "Discount: " + CURRENCY_FORMATTER.format(discount);
    }

    @Override
    protected String formatTotal(BigDecimal total) {
        return "Total: " + CURRENCY_FORMATTER.format(total);
    }

    @Override
    protected String formatAdditionalFields(Reservation reservation) {
        return "";
    }
}
