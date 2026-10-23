package com.pousadamarealta.document;

import com.pousadamarealta.domain.Document;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.Room;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

public class InternationalDocumentGenerator implements DocumentGenerator {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public Document generate(Reservation reservation, Room room) {
        Document document = new Document();
        document.setTitle("Booking receipt");
        document.setReservationId(reservation.getId());
        document.setGuestName(reservation.getGuestName());
        document.setGuestCpf(null);
        document.setCompanyCnpj(null);
        document.setCompanyName(null);
        document.setGuestPassport(reservation.getGuestPassport());
        document.setGuestCountry(reservation.getGuestCountry());
        document.setRoomNumber(room.getNumber());
        document.setRoomType(room.getType());
        document.setCheckIn(reservation.getCheckIn());
        document.setCheckOut(reservation.getCheckOut());
        document.setNights(reservation.getNights());

        BigDecimal subtotal = reservation.getSubtotal(room);
        BigDecimal discount = reservation.getDiscount(subtotal);
        BigDecimal total = reservation.getTotal(room);

        document.setSubtotal(subtotal);
        document.setDiscount(discount);
        document.setTotal(total);
        document.setText(generateText(document));

        return document;
    }

    private String generateText(Document doc) {
        StringBuilder sb = new StringBuilder();
        sb.append("Booking receipt no. ").append(doc.getReservationId()).append("\n");
        sb.append("Guest: ").append(doc.getGuestName())
          .append(" (passport ").append(doc.getGuestPassport())
          .append(", ").append(doc.getGuestCountry()).append(")\n");
        sb.append("Room: ").append(doc.getRoomNumber())
          .append(" (").append(doc.getRoomType()).append(")\n");
        sb.append("Check-in: ").append(doc.getCheckIn().format(DATE_FORMATTER))
          .append(" | Check-out: ").append(doc.getCheckOut().format(DATE_FORMATTER))
          .append(" | ").append(formatNights(doc.getNights())).append("\n");
        sb.append("Nights: ").append(formatMoney(doc.getSubtotal())).append("\n");

        if (doc.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Discount: ").append(formatMoney(doc.getDiscount())).append("\n");
        }

        sb.append("Total: ").append(formatMoney(doc.getTotal()));

        return sb.toString();
    }

    private String formatMoney(BigDecimal amount) {
        return String.format("BRL %,.2f", amount);
    }

    private String formatNights(int nights) {
        return nights == 1 ? "1 night" : nights + " nights";
    }
}
