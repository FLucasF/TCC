package com.pousadamareal.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.text.NumberFormat;
import java.util.Locale;

public class PassportIdentification implements GuestIdentification {
    private final String passport;
    private final String country;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public PassportIdentification(String passport, String country) {
        this.passport = passport;
        this.country = country;
    }

    @Override
    public String getGuestCpf() {
        return null;
    }

    @Override
    public String getCompanyCnpj() {
        return null;
    }

    @Override
    public String getCompanyName() {
        return null;
    }

    @Override
    public String getGuestPassport() {
        return passport;
    }

    @Override
    public String getGuestCountry() {
        return country;
    }

    @Override
    public Document generateDocument(long reservationId, String guestName, String roomNumber,
                                    RoomType roomType, LocalDate checkIn, LocalDate checkOut,
                                    int nights, BigDecimal subtotal, BigDecimal discount,
                                    BigDecimal total) {
        StringBuilder text = new StringBuilder();
        text.append("Booking receipt no. ").append(reservationId).append("\n");
        text.append("Guest: ").append(guestName)
            .append(" (passport ").append(passport).append(", ").append(country).append(")\n");
        text.append("Room: ").append(roomNumber).append(" (").append(roomType).append(")\n");
        text.append("Check-in: ").append(checkIn.format(DATE_FORMATTER))
            .append(" | Check-out: ").append(checkOut.format(DATE_FORMATTER))
            .append(" | ").append(nights == 1 ? "1 night" : nights + " nights").append("\n");
        text.append("Nights: ").append(formatMoney(subtotal)).append("\n");

        if (discount.compareTo(BigDecimal.ZERO) > 0) {
            text.append("Discount: ").append(formatMoney(discount)).append("\n");
        }

        text.append("Total: ").append(formatMoney(total));

        return new Document(
            "Booking receipt",
            reservationId,
            guestName,
            null,
            null,
            null,
            passport,
            country,
            roomNumber,
            roomType,
            checkIn,
            checkOut,
            nights,
            subtotal,
            discount,
            total,
            text.toString()
        );
    }

    private String formatMoney(BigDecimal amount) {
        NumberFormat nf = NumberFormat.getInstance(Locale.US);
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        return "BRL " + nf.format(amount);
    }
}
