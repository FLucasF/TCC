package br.com.pousada.maralta.guesttype;

import br.com.pousada.maralta.domain.Document;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class ForeignGuest implements GuestType {
    private final String passport;
    private final String country;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DecimalFormat CURRENCY_FORMAT;

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator(',');
        symbols.setDecimalSeparator('.');
        CURRENCY_FORMAT = new DecimalFormat("BRL #,##0.00", symbols);
    }

    public ForeignGuest(String passport, String country) {
        this.passport = passport;
        this.country = country;
    }

    @Override
    public String getGuestIdentificationLine() {
        return "passport " + passport + ", " + country;
    }

    @Override
    public Document generateDocument(
        long reservationId,
        String guestName,
        String roomNumber,
        String roomType,
        LocalDate checkIn,
        LocalDate checkOut,
        int nights,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal total
    ) {
        StringBuilder text = new StringBuilder();
        text.append("Booking receipt no. ").append(reservationId).append("\n");
        text.append("Guest: ").append(guestName)
            .append(" (passport ").append(passport).append(", ").append(country).append(")").append("\n");
        text.append("Room: ").append(roomNumber).append(" (").append(roomType).append(")").append("\n");
        text.append("Check-in: ").append(checkIn.format(DATE_FORMAT))
            .append(" | Check-out: ").append(checkOut.format(DATE_FORMAT))
            .append(" | ").append(nights).append(nights == 1 ? " night" : " nights").append("\n");
        text.append("Nights: ").append(CURRENCY_FORMAT.format(subtotal)).append("\n");

        if (discount.compareTo(BigDecimal.ZERO) > 0) {
            text.append("Discount: ").append(CURRENCY_FORMAT.format(discount)).append("\n");
        }

        text.append("Total: ").append(CURRENCY_FORMAT.format(total));

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
}
