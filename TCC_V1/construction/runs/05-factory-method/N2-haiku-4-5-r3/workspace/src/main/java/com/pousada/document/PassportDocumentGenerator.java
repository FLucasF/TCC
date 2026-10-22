package com.pousada.document;

import com.pousada.domain.Reservation;
import com.pousada.domain.Room;
import com.pousada.util.Formatter;

public class PassportDocumentGenerator implements DocumentGenerator {
    @Override
    public Document generate(Reservation reservation, Room room) {
        String text = buildText(reservation, room);

        return new Document(
            "Booking receipt",
            reservation.getId(),
            reservation.getGuestName(),
            null,
            null,
            null,
            reservation.getGuestPassport(),
            reservation.getGuestCountry(),
            reservation.getRoomNumber(),
            room.type().name(),
            reservation.getCheckIn(),
            reservation.getCheckOut(),
            reservation.getNights(),
            reservation.getSubtotal(),
            reservation.getDiscount(),
            reservation.getTotal(),
            text
        );
    }

    private String buildText(Reservation reservation, Room room) {
        StringBuilder sb = new StringBuilder();
        sb.append("Booking receipt no. ").append(reservation.getId()).append("\n");
        sb.append("Guest: ").append(reservation.getGuestName());
        sb.append(" (passport ").append(reservation.getGuestPassport());
        sb.append(", ").append(reservation.getGuestCountry()).append(")\n");
        sb.append("Room: ").append(reservation.getRoomNumber());
        sb.append(" (").append(room.type().name()).append(")\n");

        String nightsLabel = reservation.getNights() == 1 ? "1 night" :
                            reservation.getNights() + " nights";
        sb.append("Check-in: ").append(Formatter.formatEnDate(reservation.getCheckIn()));
        sb.append(" | Check-out: ").append(Formatter.formatEnDate(reservation.getCheckOut()));
        sb.append(" | ").append(nightsLabel).append("\n");

        sb.append("Nights: ").append(Formatter.formatEnCurrency(reservation.getSubtotal())).append("\n");

        if (reservation.getDiscount().signum() > 0) {
            sb.append("Discount: ").append(Formatter.formatEnCurrency(reservation.getDiscount())).append("\n");
        }

        sb.append("Total: ").append(Formatter.formatEnCurrency(reservation.getTotal()));

        return sb.toString();
    }
}
