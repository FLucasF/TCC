package br.com.mareaalta.domain;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class PassportDocumentGenerator implements DocumentGenerator {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DecimalFormat CURRENCY_FORMAT;

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator(',');
        symbols.setDecimalSeparator('.');
        CURRENCY_FORMAT = new DecimalFormat("BRL #,##0.00", symbols);
    }

    @Override
    public void validate(Reservation reservation) {
        if (reservation.getGuestPassport() == null || reservation.getGuestPassport().isBlank()) {
            throw new IllegalArgumentException("Passport is required");
        }
        if (reservation.getGuestCountry() == null || reservation.getGuestCountry().isBlank()) {
            throw new IllegalArgumentException("Country is required");
        }
    }

    @Override
    public DocumentData generateDocument(Reservation reservation) {
        DocumentData doc = new DocumentData();
        doc.setTitle("Booking receipt");
        doc.setReservationId(reservation.getId());
        doc.setGuestName(reservation.getGuestName());
        doc.setGuestCpf(null);
        doc.setCompanyCnpj(null);
        doc.setCompanyName(null);
        doc.setGuestPassport(reservation.getGuestPassport());
        doc.setGuestCountry(reservation.getGuestCountry());
        doc.setRoomNumber(reservation.getRoomNumber());
        doc.setRoomType(reservation.getRoom().getType());
        doc.setCheckIn(reservation.getCheckIn().format(DATE_FORMAT));
        doc.setCheckOut(reservation.getCheckOut().format(DATE_FORMAT));
        doc.setNights(reservation.getNights());
        doc.setSubtotal(reservation.getSubtotal());
        doc.setDiscount(reservation.getDiscount());
        doc.setTotal(reservation.getTotal());
        doc.setText(generateText(doc));
        return doc;
    }

    private String generateText(DocumentData doc) {
        StringBuilder sb = new StringBuilder();
        sb.append("Booking receipt no. ").append(doc.getReservationId()).append("\n");
        sb.append("Guest: ").append(doc.getGuestName())
          .append(" (passport ").append(doc.getGuestPassport())
          .append(", ").append(doc.getGuestCountry()).append(")\n");
        sb.append("Room: ").append(doc.getRoomNumber())
          .append(" (").append(doc.getRoomType()).append(")\n");
        sb.append("Check-in: ").append(doc.getCheckIn())
          .append(" | Check-out: ").append(doc.getCheckOut())
          .append(" | ").append(doc.getNights()).append(doc.getNights() == 1 ? " night" : " nights").append("\n");
        sb.append("Nights: ").append(formatCurrency(doc.getSubtotal())).append("\n");

        if (doc.getDiscount() != null && doc.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Discount: ").append(formatCurrency(doc.getDiscount())).append("\n");
        }

        sb.append("Total: ").append(formatCurrency(doc.getTotal()));
        return sb.toString();
    }

    private String formatCurrency(BigDecimal amount) {
        return CURRENCY_FORMAT.format(amount);
    }
}
