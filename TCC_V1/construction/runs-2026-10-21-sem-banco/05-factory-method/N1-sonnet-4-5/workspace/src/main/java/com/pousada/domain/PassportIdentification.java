package com.pousada.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PassportIdentification(
    @JsonProperty("guestName") String guestName,
    @JsonProperty("guestPassport") String guestPassport,
    @JsonProperty("guestCountry") String guestCountry
) implements GuestIdentification {

    @Override
    public String getGuestName() {
        return guestName;
    }

    @Override
    public String formatForDocument() {
        return guestName + " (passport " + guestPassport + ", " + guestCountry + ")";
    }

    @Override
    public String generateDocument(Document.DocumentData data) {
        StringBuilder doc = new StringBuilder();
        doc.append("Booking receipt no. ").append(data.reservationId()).append("\n");
        doc.append("Guest: ").append(formatForDocument()).append("\n");
        doc.append("Room: ").append(data.roomNumber()).append(" (").append(data.roomType()).append(")\n");

        String checkInFormatted = data.checkIn().replace('/', '-');
        String[] parts = checkInFormatted.split("-");
        checkInFormatted = parts[2] + "-" + parts[1] + "-" + parts[0];

        String checkOutFormatted = data.checkOut().replace('/', '-');
        parts = checkOutFormatted.split("-");
        checkOutFormatted = parts[2] + "-" + parts[1] + "-" + parts[0];

        doc.append("Check-in: ").append(checkInFormatted).append(" | Check-out: ").append(checkOutFormatted);
        doc.append(" | ").append(data.nights()).append(data.nights() == 1 ? " night" : " nights").append("\n");

        doc.append("Nights: BRL ").append(formatMoneyEnglish(data.subtotal())).append("\n");

        if (data.discount() != null && data.discount().compareTo(java.math.BigDecimal.ZERO) > 0) {
            doc.append("Discount: BRL ").append(formatMoneyEnglish(data.discount())).append("\n");
        }

        doc.append("Total: BRL ").append(formatMoneyEnglish(data.total()));

        return doc.toString();
    }

    private String formatMoneyEnglish(java.math.BigDecimal value) {
        return String.format("%,.2f", value);
    }
}
