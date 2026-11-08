package br.com.marealta.web;

import br.com.marealta.domain.Reservation;
import br.com.marealta.domain.Room;
import java.math.BigDecimal;

/** Formato JSON combinado com o site. */
final class Views {
    private Views() {}

    record RoomView(int number, String type, int capacity, BigDecimal nightlyRate) {}

    record ReservationView(long id, int roomNumber, String guestName, String guestEmail, String guestCpf,
                           String checkIn, String checkOut, int guests, String ratePlan, int nights,
                           BigDecimal subtotal, BigDecimal discount, BigDecimal total, String status,
                           BigDecimal amountPaid, BigDecimal refund) {}

    record ErrorView(String error, String message) {}

    static RoomView of(Room r) {
        return new RoomView(r.number(), r.type().name(), r.capacity(), r.nightlyRate());
    }

    static ReservationView of(Reservation r) {
        return new ReservationView(r.id(), r.roomNumber(), r.guestName(), r.guestEmail(), r.guestCpf(),
                r.checkIn().toString(), r.checkOut().toString(), r.guests(), r.ratePlan().name(), r.nights(),
                r.subtotal(), r.discount(), r.total(), r.status().name(), r.amountPaid(), r.refund());
    }
}
