package pousada.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Receipt(
        String title,
        int reservationId,
        String guestName,
        String guestCpf,
        int roomNumber,
        RoomType roomType,
        LocalDate checkIn,
        LocalDate checkOut,
        int nights,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal total) {

    public static Receipt of(Reservation r) {
        Quote q = r.quote();
        return new Receipt("Comprovante de reserva", r.id(), r.guestName(), r.guestCpf(), r.room().number(),
                r.room().type(), r.checkIn(), r.checkOut(), q.nights(), q.subtotal(), q.discount(), q.total());
    }
}
