package com.pousadamareaalta.document;

import com.pousadamareaalta.model.Document;
import com.pousadamareaalta.model.Reservation;
import com.pousadamareaalta.model.Room;
import com.pousadamareaalta.util.DateUtils;

import java.math.BigDecimal;

public class PassportBookingReceipt implements DocumentGenerator {
    @Override
    public Document generate(Reservation reservation, Room room) {
        String checkInStr = DateUtils.formatDateEnglish(reservation.getCheckIn());
        String checkOutStr = DateUtils.formatDateEnglish(reservation.getCheckOut());
        String nightsStr = getNightsLabel(reservation.getNights());
        String subtotalStr = formatMoney(reservation.getSubtotal());
        String discountStr = reservation.getDiscount().compareTo(BigDecimal.ZERO) > 0
            ? formatMoney(reservation.getDiscount())
            : null;
        String totalStr = formatMoney(reservation.getTotal());

        StringBuilder sb = new StringBuilder();
        sb.append("Booking receipt no. ").append(reservation.getId()).append("\n");
        sb.append("Guest: ").append(reservation.getGuestName())
            .append(" (passport ").append(reservation.getGuestPassport())
            .append(", ").append(reservation.getGuestCountry()).append(")\n");
        sb.append("Room: ").append(reservation.getRoomNumber())
            .append(" (").append(room.type()).append(")\n");
        sb.append("Check-in: ").append(checkInStr)
            .append(" | Check-out: ").append(checkOutStr)
            .append(" | ").append(nightsStr).append("\n");
        sb.append("Nights: ").append(subtotalStr).append("\n");
        if (discountStr != null) {
            sb.append("Discount: ").append(discountStr).append("\n");
        }
        sb.append("Total: ").append(totalStr);

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
            room.type().toString(),
            checkInStr,
            checkOutStr,
            reservation.getNights(),
            reservation.getSubtotal(),
            reservation.getDiscount(),
            reservation.getTotal(),
            sb.toString()
        );
    }

    private String getNightsLabel(int nights) {
        return nights == 1 ? "1 night" : nights + " nights";
    }

    private String formatMoney(BigDecimal value) {
        return String.format(java.util.Locale.US, "BRL %,.2f", value);
    }
}
