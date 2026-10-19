package br.com.marealta.service;

import br.com.marealta.domain.Reservation;

import java.math.BigDecimal;

/** Comprovante de reserva: dados + texto. */
public record Receipt(String title, int reservationId, String guestName, String guestCpf, int roomNumber,
                      String roomType, String checkIn, String checkOut, int nights, BigDecimal subtotal,
                      BigDecimal discount, BigDecimal total) {
    public static final String TITLE = "Comprovante de reserva";

    public static Receipt of(Reservation r) {
        return new Receipt(TITLE, r.id(), r.guestName(), r.guestCpf(), r.room().number(),
                r.room().type().name(), r.checkInDate().toString(), r.checkOutDate().toString(),
                r.nights(), r.subtotal(), r.discount(), r.total());
    }
}
