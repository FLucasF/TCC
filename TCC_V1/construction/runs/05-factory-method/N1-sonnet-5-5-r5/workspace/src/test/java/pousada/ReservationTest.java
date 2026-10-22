package pousada;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;
import pousada.domain.*;

class ReservationTest {
    private Reservation pending(RatePlan plan) {
        var room = Rooms.find(201).orElseThrow();
        var q = Pricing.quote(room, LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), plan);
        return new Reservation(1, 201, "Carla", "c@x.com", new Identity("CPF", Map.of("guestCpf", "1")),
                LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), 2, plan, q.nights(), q.subtotal(),
                q.discount(), q.total(), ReservationStatus.PENDING, Money.ZERO, null);
    }

    @Test
    void confirmationPaysTheTotal() {
        var r = pending(RatePlan.STANDARD).confirm();
        assertEquals(ReservationStatus.CONFIRMED, r.status());
        assertEquals(r.total(), r.amountPaid());
    }

    @Test
    void cancelRefundsOnlyStandardPlan() {
        assertEquals(Money.of("779.80"), pending(RatePlan.STANDARD).confirm().cancel().refund());
        assertEquals(Money.ZERO, pending(RatePlan.NON_REFUNDABLE).confirm().cancel().refund());
        assertEquals(Money.ZERO, pending(RatePlan.STANDARD).cancel().refund());
    }

    @Test
    void retainedKeepsNonRefundableCancellation() {
        var r = pending(RatePlan.NON_REFUNDABLE).confirm().cancel();
        assertEquals(r.total(), r.retained());
    }

    @Test
    void invalidTransitionsAreRejected() {
        assertThrows(ApiException.class, () -> pending(RatePlan.STANDARD).startStay());
        assertThrows(ApiException.class, () -> pending(RatePlan.STANDARD).endStay());
        assertThrows(ApiException.class, () -> pending(RatePlan.STANDARD).confirm().confirm());
        assertThrows(ApiException.class, () -> pending(RatePlan.STANDARD).confirm().startStay().cancel());
        assertThrows(ApiException.class, () -> pending(RatePlan.STANDARD).cancel().confirm());
    }
}
