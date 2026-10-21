package br.com.pousadamarealta.reservas.service;

import br.com.pousadamarealta.reservas.domain.Reservation;
import br.com.pousadamarealta.reservas.domain.RoomType;
import java.math.BigDecimal;
import java.time.LocalDate;

public record Receipt(String title, long reservationId, String guestName, String guestCpf, int roomNumber,
                      RoomType roomType, LocalDate checkIn, LocalDate checkOut, int nights,
                      BigDecimal subtotal, BigDecimal discount, BigDecimal total) {

    public static final String TITLE = "Comprovante de reserva";

    public static Receipt of(Reservation r) {
        return new Receipt(TITLE, r.id(), r.guestName(), r.guestCpf(), r.room().number(), r.room().type(),
                r.checkIn(), r.checkOut(), r.quote().nights(), r.quote().subtotal(), r.quote().discount(),
                r.quote().total());
    }
}
