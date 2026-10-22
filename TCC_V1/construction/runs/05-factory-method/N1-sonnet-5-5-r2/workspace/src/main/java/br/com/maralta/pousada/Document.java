package br.com.maralta.pousada;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Documento de uma reserva paga: comprovante, fatura ou booking receipt, conforme a identificação. */
public record Document(
        long reservationId,
        String guestName,
        Identification identification,
        int roomNumber,
        RoomType roomType,
        LocalDate checkIn,
        LocalDate checkOut,
        int nights,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal total) {

    public static Document of(Reservation reservation) {
        Room room = Rooms.get(reservation.roomNumber());
        return new Document(reservation.id(), reservation.guestName(), reservation.identification(),
                room.number(), room.type(), reservation.checkIn(), reservation.checkOut(), reservation.nights(),
                reservation.subtotal(), reservation.discount(), reservation.total());
    }

    public String title() {
        return identification.title();
    }

    public String text() {
        Language lang = identification.language();
        List<String> lines = new ArrayList<>();
        lines.add(title() + " " + lang.numberLabel() + " " + reservationId);
        lines.addAll(identification.partyLines(guestName));
        lines.add(lang.room() + ": " + roomNumber + " (" + roomType + ")");
        lines.add(lang.checkIn() + ": " + lang.date(checkIn) + " | " + lang.checkOut() + ": " + lang.date(checkOut)
                + " | " + lang.nights(nights));
        lines.add(lang.items() + ": " + lang.money(subtotal));
        if (discount.signum() > 0) {
            lines.add(lang.discount() + ": " + lang.money(discount));
        }
        lines.add(lang.total() + ": " + lang.money(total));
        lines.addAll(identification.footerLines(this));
        return String.join("\n", lines);
    }

    public Map<String, Object> toJson() {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("title", title());
        json.put("reservationId", reservationId);
        json.put("guestName", guestName);
        json.putAll(Identifications.allFields(identification));
        json.put("roomNumber", roomNumber);
        json.put("roomType", roomType);
        json.put("checkIn", checkIn.toString());
        json.put("checkOut", checkOut.toString());
        json.put("nights", nights);
        json.put("subtotal", subtotal);
        json.put("discount", discount);
        json.put("total", total);
        json.put("text", text());
        return json;
    }
}
