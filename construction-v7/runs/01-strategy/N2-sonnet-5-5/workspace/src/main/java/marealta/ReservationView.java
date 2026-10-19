package marealta;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ReservationView(int id, int roomNumber, String guestName, String guestEmail, String guestCpf,
                              LocalDate checkIn, LocalDate checkOut, int guests, String ratePlan, int nights,
                              BigDecimal subtotal, BigDecimal discount, BigDecimal total,
                              ReservationStatus status, BigDecimal amountPaid, BigDecimal refund) {
    public static ReservationView of(Reservation r) {
        return new ReservationView(r.id(), r.room().number(), r.guestName(), r.guestEmail(), r.guestCpf(),
                r.checkInDate(), r.checkOutDate(), r.guests(), r.plan().code(), r.nights(),
                r.subtotal(), r.discount(), r.total(), r.status(), r.amountPaid(), r.refund());
    }
}
