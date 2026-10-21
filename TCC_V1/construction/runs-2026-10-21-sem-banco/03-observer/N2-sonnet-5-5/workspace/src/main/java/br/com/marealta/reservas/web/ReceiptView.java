package br.com.marealta.reservas.web;

import br.com.marealta.reservas.domain.Receipt;
import br.com.marealta.reservas.domain.RoomType;

import java.math.BigDecimal;

public record ReceiptView(String title, int reservationId, String guestName, String guestCpf, int roomNumber,
                          RoomType roomType, String checkIn, String checkOut, int nights,
                          BigDecimal subtotal, BigDecimal discount, BigDecimal total) {

    static ReceiptView of(Receipt r) {
        return new ReceiptView(r.title(), r.reservationId(), r.guestName(), r.guestCpf(), r.roomNumber(),
                r.roomType(), r.checkIn().toString(), r.checkOut().toString(), r.nights(),
                r.subtotal(), r.discount(), r.total());
    }
}
