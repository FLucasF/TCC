package com.pousadamaralta.domain.identification;

import com.pousadamaralta.domain.model.RoomType;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class PassportIdentification extends GuestIdentification {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final String passport;
    private final String country;

    public PassportIdentification(String passport, String country) {
        this.passport = passport;
        this.country = country;
    }

    @Override
    protected String getDocumentTitle() {
        return "Booking receipt";
    }

    @Override
    protected String formatGuestLine(String guestName) {
        return "Guest: " + guestName + " (passport " + passport + ", " + country + ")";
    }

    @Override
    protected String formatRoomLine(String roomNumber, RoomType roomType) {
        return "Room: " + roomNumber + " (" + roomType.name() + ")";
    }

    @Override
    protected String formatStayLine(LocalDate checkIn, LocalDate checkOut, int nights) {
        String nightsLabel = nights == 1 ? "1 night" : nights + " nights";
        return "Check-in: " + formatDate(checkIn) + " | Check-out: " + formatDate(checkOut) + " | " + nightsLabel;
    }

    @Override
    protected String formatSubtotalLine(BigDecimal subtotal, int nights) {
        return "Nights: " + formatMoney(subtotal);
    }

    @Override
    protected String formatDiscountLine(BigDecimal discount) {
        return "Discount: " + formatMoney(discount);
    }

    @Override
    protected String formatTotalLine(BigDecimal total) {
        return "Total: " + formatMoney(total);
    }

    @Override
    protected String formatMoney(BigDecimal amount) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator(',');
        symbols.setDecimalSeparator('.');
        DecimalFormat df = new DecimalFormat("BRL #,##0.00", symbols);
        return df.format(amount);
    }

    @Override
    protected String formatDate(LocalDate date) {
        return date.format(DATE_FORMATTER);
    }

    @Override
    public String getCpf() {
        return null;
    }

    @Override
    public String getCnpj() {
        return null;
    }

    @Override
    public String getCompanyName() {
        return null;
    }

    @Override
    public String getPassport() {
        return passport;
    }

    @Override
    public String getCountry() {
        return country;
    }
}
