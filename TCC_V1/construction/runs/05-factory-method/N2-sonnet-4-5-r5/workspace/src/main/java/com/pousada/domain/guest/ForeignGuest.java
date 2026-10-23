package com.pousada.domain.guest;

import com.pousada.domain.model.Document;
import com.pousada.domain.model.Reservation;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

public final class ForeignGuest implements GuestType {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final String passport;
    private final String country;

    public ForeignGuest(String passport, String country) {
        this.passport = passport;
        this.country = country;
    }

    @Override
    public Document generateDocument(Reservation reservation) {
        String text = buildText(reservation);

        return Document.builder()
            .title("Booking receipt")
            .reservationId(reservation.id())
            .guestName(reservation.guestName())
            .guestCpf(null)
            .companyCnpj(null)
            .companyName(null)
            .guestPassport(passport)
            .guestCountry(country)
            .roomNumber(reservation.roomNumber())
            .roomType(reservation.roomType().name())
            .checkIn(reservation.checkIn())
            .checkOut(reservation.checkOut())
            .nights(reservation.nights())
            .subtotal(reservation.subtotal())
            .discount(reservation.discount())
            .total(reservation.total())
            .text(text)
            .build();
    }

    private String buildText(Reservation r) {
        StringBuilder sb = new StringBuilder();
        sb.append("Booking receipt no. ").append(r.id()).append("\n");
        sb.append("Guest: ").append(r.guestName()).append(" (passport ").append(passport)
          .append(", ").append(country).append(")\n");
        sb.append("Room: ").append(r.roomNumber()).append(" (").append(roomTypeName(r)).append(")\n");
        sb.append("Check-in: ").append(r.checkIn().format(DATE_FORMAT));
        sb.append(" | Check-out: ").append(r.checkOut().format(DATE_FORMAT));
        sb.append(" | ").append(nightsText(r.nights())).append("\n");
        sb.append("Nights: ").append(formatMoney(r.subtotal())).append("\n");

        if (r.discount() != null && r.discount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Discount: ").append(formatMoney(r.discount())).append("\n");
        }

        sb.append("Total: ").append(formatMoney(r.total()));

        return sb.toString();
    }

    private String roomTypeName(Reservation r) {
        return r.roomType().name();
    }

    private String nightsText(int nights) {
        return nights == 1 ? "1 night" : nights + " nights";
    }

    private String formatMoney(BigDecimal amount) {
        String formatted = String.format("%.2f", amount);
        String[] parts = formatted.split("\\.");
        String integerPart = parts[0];
        String decimalPart = parts[1];

        StringBuilder result = new StringBuilder();
        int len = integerPart.length();
        for (int i = 0; i < len; i++) {
            if (i > 0 && (len - i) % 3 == 0) {
                result.append(',');
            }
            result.append(integerPart.charAt(i));
        }

        return "BRL " + result.toString() + "." + decimalPart;
    }

    @Override
    public String guestCpf() {
        return null;
    }

    @Override
    public String companyCnpj() {
        return null;
    }

    @Override
    public String companyName() {
        return null;
    }

    @Override
    public String guestPassport() {
        return passport;
    }

    @Override
    public String guestCountry() {
        return country;
    }
}
