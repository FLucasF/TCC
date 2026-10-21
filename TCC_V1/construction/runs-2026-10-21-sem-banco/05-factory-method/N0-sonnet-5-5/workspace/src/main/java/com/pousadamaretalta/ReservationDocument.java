package com.pousadamaretalta;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Instantâneo dos dados de uma reserva paga, a partir do qual o documento é redigido. */
public record ReservationDocument(
        int reservationId, String guestName, GuestIdentity identity,
        int roomNumber, RoomType roomType, LocalDate checkIn, LocalDate checkOut,
        int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total) {

    public String title() { return identity.template().title(); }

    public String text() { return identity.template().render(this); }
}
