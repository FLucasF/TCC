package br.com.mealta.web;

import br.com.mealta.domain.Message;
import br.com.mealta.domain.Receipt;
import br.com.mealta.domain.Reservation;
import br.com.mealta.domain.Room;

import java.math.BigDecimal;

/** O jeito como o site enxerga cada coisa: datas como texto, dinheiro como número com 2 casas. */
public final class Views {
    private Views() {
    }

    public record RoomView(int number, String type, int capacity, BigDecimal nightlyRate) {
    }

    public record ReservationView(long id, int roomNumber, String guestName, String guestEmail, String guestCpf,
                                  String checkIn, String checkOut, int guests, String ratePlan, int nights,
                                  BigDecimal subtotal, BigDecimal discount, BigDecimal total, String status,
                                  BigDecimal amountPaid, BigDecimal refund) {
    }

    public record ReceiptView(String title, long reservationId, String guestName, String guestCpf,
                              int roomNumber, String roomType, String checkIn, String checkOut, int nights,
                              BigDecimal subtotal, BigDecimal discount, BigDecimal total) {
    }

    public record MessageView(long id, String channel, String to, String subject, String body) {
    }

    public static RoomView of(Room r) {
        return new RoomView(r.number(), r.type().name(), r.capacity(), r.nightlyRate());
    }

    public static ReservationView of(Reservation r) {
        return new ReservationView(r.id(), r.roomNumber(), r.guestName(), r.guestEmail(), r.guestCpf(),
                r.checkInDate().toString(), r.checkOutDate().toString(), r.guests(), r.ratePlan().name(),
                r.nights(), r.subtotal(), r.discount(), r.total(), r.status().name(), r.amountPaid(),
                r.refund());
    }

    public static ReceiptView of(Receipt r) {
        return new ReceiptView(r.title(), r.reservationId(), r.guestName(), r.guestCpf(), r.roomNumber(),
                r.roomType().name(), r.checkIn().toString(), r.checkOut().toString(), r.nights(),
                r.subtotal(), r.discount(), r.total());
    }

    public static MessageView of(Message m) {
        return new MessageView(m.id(), m.channel().name(), m.to(), m.subject(), m.body());
    }
}
