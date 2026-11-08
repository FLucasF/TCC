package br.com.marealta.pousada.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Receipt(String title, long reservationId, String guestName, String guestCpf, int roomNumber,
                      RoomType roomType, LocalDate checkIn, LocalDate checkOut, int nights,
                      BigDecimal subtotal, BigDecimal discount, BigDecimal total) {

    public static Receipt of(Reservation r) {
        Quote q = r.quote();
        return new Receipt("Comprovante de reserva", r.id(), r.guest().name(), r.guest().cpf(),
                r.room().number(), r.room().type(), r.checkIn(), r.checkOut(), q.nights(),
                q.subtotal(), q.discount(), q.total());
    }
}
