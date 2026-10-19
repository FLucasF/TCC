package br.com.marealta.service;

import br.com.marealta.domain.Reservation;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Receipt(String title, int reservationId, String guestName, String guestCpf, int roomNumber,
                      String roomType, LocalDate checkIn, LocalDate checkOut, int nights,
                      BigDecimal subtotal, BigDecimal discount, BigDecimal total) {

    public static final String TITLE = "Comprovante de reserva";

    public static Receipt of(Reservation r) {
        return new Receipt(TITLE, r.id(), r.guestName(), r.guestCpf(), r.room().number(),
                r.room().type().name(), r.checkIn(), r.checkOut(), r.quote().nights(),
                r.quote().subtotal(), r.quote().discount(), r.quote().total());
    }
}
