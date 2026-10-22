package br.com.marealta.pousada.web;

import br.com.marealta.pousada.domain.RatePlan;
import br.com.marealta.pousada.domain.Reservation;
import br.com.marealta.pousada.domain.ReservationStatus;
import br.com.marealta.pousada.domain.Room;
import br.com.marealta.pousada.domain.RoomType;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Os formatos JSON combinados com o site. */
final class Views {
    private Views() {
    }

    record RoomView(int number, RoomType type, int capacity, BigDecimal nightlyRate) {
        static RoomView of(Room room) {
            return new RoomView(room.number(), room.type(), room.capacity(), room.nightlyRate());
        }
    }

    record ReservationView(long id, int roomNumber, String guestName, String guestEmail, String guestCpf,
                           LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan, int nights,
                           BigDecimal subtotal, BigDecimal discount, BigDecimal total, ReservationStatus status,
                           BigDecimal amountPaid, BigDecimal refund) {
        static ReservationView of(Reservation r) {
            return new ReservationView(r.id(), r.room().number(), r.guestName(), r.guestEmail(), r.guestCpf(),
                    r.checkIn(), r.checkOut(), r.guests(), r.ratePlan(), r.nights(), r.subtotal(), r.discount(),
                    r.total(), r.status(), r.amountPaid(), r.refund());
        }
    }

    record ErrorView(String error, String message) {
    }
}
