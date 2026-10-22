package br.com.marealta.reservas.api;

import br.com.marealta.reservas.domain.Receipt;
import br.com.marealta.reservas.domain.Reservation;
import br.com.marealta.reservas.domain.Room;

import java.math.BigDecimal;

/** Formato JSON combinado com o site. */
final class Views {
    private Views() {
    }

    record RoomView(int number, String type, int capacity, BigDecimal nightlyRate) {
        static RoomView of(Room room) {
            return new RoomView(room.number(), room.type().name(), room.capacity(), room.nightlyRate());
        }
    }

    record ReservationView(long id, int roomNumber, String guestName, String guestEmail, String guestCpf,
                           String checkIn, String checkOut, int guests, String ratePlan, int nights,
                           BigDecimal subtotal, BigDecimal discount, BigDecimal total, String status,
                           BigDecimal amountPaid, BigDecimal refund) {
        static ReservationView of(Reservation r) {
            return new ReservationView(r.id(), r.roomNumber(), r.guestName(), r.guestEmail(), r.guestCpf(),
                    r.checkIn().toString(), r.checkOut().toString(), r.guests(), r.ratePlan().name(),
                    r.quote().nights(), r.quote().subtotal(), r.quote().discount(), r.quote().total(),
                    r.status().name(), r.amountPaid(), r.refund());
        }
    }

    record ReceiptView(String title, long reservationId, String guestName, String guestCpf, int roomNumber,
                       String roomType, String checkIn, String checkOut, int nights, BigDecimal subtotal,
                       BigDecimal discount, BigDecimal total) {
        static ReceiptView of(Receipt r) {
            return new ReceiptView(Receipt.TITLE, r.reservationId(), r.guestName(), r.guestCpf(), r.roomNumber(),
                    r.roomType().name(), r.checkIn().toString(), r.checkOut().toString(), r.nights(),
                    r.subtotal(), r.discount(), r.total());
        }
    }

    record ErrorView(String error, String message) {
    }
}
