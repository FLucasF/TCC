package com.pousada.document;

import com.pousada.domain.Reservation;
import com.pousada.domain.Room;
import java.time.format.DateTimeFormatter;

public class PassportReceiptRenderer implements DocumentRenderer {
    private final Room room;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public PassportReceiptRenderer(Room room) {
        this.room = room;
    }

    @Override
    public DocumentContent render(Reservation reservation) {
        String checkInFormatted = reservation.getCheckIn().format(DATE_FORMAT);
        String checkOutFormatted = reservation.getCheckOut().format(DATE_FORMAT);

        String nightsLabel = reservation.getNights() == 1 ? "1 night" : reservation.getNights() + " nights";

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Booking receipt no. %d\n", reservation.getId()));
        sb.append(String.format("Guest: %s (passport %s, %s)\n", reservation.getGuestName(),
                reservation.getGuestPassport(), reservation.getGuestCountry()));
        sb.append(String.format("Room: %d (%s)\n", reservation.getRoomNumber(), room.type.name));
        sb.append(String.format("Check-in: %s | Check-out: %s | %s\n", checkInFormatted, checkOutFormatted,
                nightsLabel));
        sb.append(String.format("Nights: BRL %,.2f\n", reservation.getSubtotal()).replace(",", "COMMA")
                .replace(".", ",").replace("COMMA", "."));

        if (reservation.getDiscount() > 0) {
            sb.append(String.format("Discount: BRL %,.2f\n", reservation.getDiscount()).replace(",", "COMMA")
                    .replace(".", ",").replace("COMMA", "."));
        }

        sb.append(String.format("Total: BRL %,.2f", reservation.getTotal()).replace(",", "COMMA")
                .replace(".", ",").replace("COMMA", "."));

        String text = sb.toString();

        return new DocumentContent(
                "Booking receipt",
                reservation.getId(),
                reservation.getGuestName(),
                null,
                null,
                null,
                reservation.getGuestPassport(),
                reservation.getGuestCountry(),
                reservation.getRoomNumber(),
                room.type.name,
                checkInFormatted,
                checkOutFormatted,
                reservation.getNights(),
                reservation.getSubtotal(),
                reservation.getDiscount(),
                reservation.getTotal(),
                text);
    }
}
