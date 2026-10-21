package pousada.web;

import java.math.BigDecimal;
import pousada.domain.RatePlan;
import pousada.domain.Reservation;
import pousada.domain.ReservationStatus;
import pousada.domain.Room;
import pousada.domain.RoomType;

public final class Views {
    private Views() {
    }

    public record RoomView(int number, RoomType type, int capacity, BigDecimal nightlyRate) {
    }

    public record ReservationView(
            int id, int roomNumber, String guestName, String guestEmail, String guestCpf,
            String checkIn, String checkOut, int guests, RatePlan ratePlan, int nights,
            BigDecimal subtotal, BigDecimal discount, BigDecimal total, ReservationStatus status,
            BigDecimal amountPaid, BigDecimal refund) {
    }

    public static RoomView of(Room room) {
        return new RoomView(room.number(), room.type(), room.capacity(), room.nightlyRate());
    }

    public static ReservationView of(Reservation r) {
        return new ReservationView(r.id(), r.room().number(), r.guestName(), r.guestEmail(), r.guestCpf(),
                r.checkIn().toString(), r.checkOut().toString(), r.guests(), r.ratePlan(), r.nights(),
                r.quote().subtotal(), r.quote().discount(), r.quote().total(), r.status(),
                r.amountPaid(), r.refund());
    }
}
