package br.com.marealta;

import java.math.BigDecimal;
import java.time.LocalDate;

/** The guest-facing document (receipt, invoice, booking receipt) with its rendered text. */
record Document(String title, Reservation reservation, String text) {
    int reservationId() { return reservation.id; }
    String guestName() { return reservation.guestName; }
    Identification identification() { return reservation.identification; }
    Room room() { return reservation.room; }
    LocalDate checkIn() { return reservation.checkIn; }
    LocalDate checkOut() { return reservation.checkOut; }
    int nights() { return reservation.quote.nights(); }
    BigDecimal subtotal() { return reservation.quote.subtotal(); }
    BigDecimal discount() { return reservation.quote.discount(); }
    BigDecimal total() { return reservation.quote.total(); }
}
