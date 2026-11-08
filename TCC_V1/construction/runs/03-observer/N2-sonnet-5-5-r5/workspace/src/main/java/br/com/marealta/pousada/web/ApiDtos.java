package br.com.marealta.pousada.web;

import br.com.marealta.pousada.domain.Receipt;
import br.com.marealta.pousada.domain.Reservation;
import br.com.marealta.pousada.domain.Room;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Formas JSON combinadas com o site. */
final class ApiDtos {
    private ApiDtos() {
    }

    record RoomJson(int number, String type, int capacity, BigDecimal nightlyRate) {
        static RoomJson of(Room r) {
            return new RoomJson(r.number(), r.type().name(), r.capacity(), r.nightlyRate());
        }
    }

    record ReservationJson(long id, int roomNumber, String guestName, String guestEmail, String guestCpf,
                           LocalDate checkIn, LocalDate checkOut, int guests, String ratePlan, int nights,
                           BigDecimal subtotal, BigDecimal discount, BigDecimal total, String status,
                           BigDecimal amountPaid, BigDecimal refund) {
        static ReservationJson of(Reservation r) {
            return new ReservationJson(r.id(), r.room().number(), r.guestName(), r.guestEmail(), r.guestCpf(),
                    r.checkIn(), r.checkOut(), r.guests(), r.ratePlan().name(), r.nights(),
                    r.subtotal(), r.discount(), r.total(), r.status().name(), r.amountPaid(), r.refund());
        }
    }

    record ReceiptJson(String title, long reservationId, String guestName, String guestCpf, int roomNumber,
                       String roomType, LocalDate checkIn, LocalDate checkOut, int nights,
                       BigDecimal subtotal, BigDecimal discount, BigDecimal total) {
        static ReceiptJson of(Receipt r) {
            return new ReceiptJson(Receipt.TITLE, r.reservationId(), r.guestName(), r.guestCpf(), r.roomNumber(),
                    r.roomType().name(), r.checkIn(), r.checkOut(), r.nights(), r.subtotal(), r.discount(), r.total());
        }
    }

    record ErrorJson(String error, String message) {
    }
}
