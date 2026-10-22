package com.pousadamareaalta.documents;

import com.pousadamareaalta.models.Reservation;
import com.pousadamareaalta.models.Room;

public class PassportDocument implements DocumentGenerator {
    @Override
    public String getTitle() {
        return "Booking receipt";
    }

    @Override
    public String generate(Reservation reservation, Room room, long reservationId) {
        StringBuilder sb = new StringBuilder();
        sb.append("Booking receipt no. ").append(reservationId).append("\n");
        sb.append("Guest: ").append(reservation.guestName).append(" (passport ").append(reservation.guestPassport).append(", ").append(reservation.guestCountry).append(")\n");
        sb.append("Room: ").append(room.number).append(" (").append(room.type).append(")\n");
        sb.append("Check-in: ").append(formatDate(reservation.checkIn));
        sb.append(" | Check-out: ").append(formatDate(reservation.checkOut));
        sb.append(" | ").append(getNightsLabel(reservation.nights)).append("\n");
        sb.append("Nights: BRL ").append(formatMoneyEnglish(reservation.subtotal)).append("\n");

        if (reservation.discount > 0) {
            sb.append("Discount: BRL ").append(formatMoneyEnglish(reservation.discount)).append("\n");
        }

        sb.append("Total: BRL ").append(formatMoneyEnglish(reservation.total));

        return sb.toString();
    }

    private String getNightsLabel(int nights) {
        if (nights == 1) {
            return "1 night";
        }
        return nights + " nights";
    }

    private String formatDate(java.time.LocalDate date) {
        return String.format("%04d-%02d-%02d", date.getYear(), date.getMonthValue(), date.getDayOfMonth());
    }

    private String formatMoneyEnglish(double value) {
        return String.format("%.2f", value);
    }
}
