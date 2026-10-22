package pousada;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import pousada.domain.Pricing;
import pousada.domain.RatePlan;
import pousada.domain.Room;
import pousada.domain.RoomType;

class PricingTest {
    private final Room standard = new Room(101, RoomType.STANDARD, 2, new BigDecimal("249.90"));

    @Test
    void highSeasonNightsCostTwentyPercentMore() {
        var q = Pricing.quote(standard, LocalDate.parse("2027-07-10"), LocalDate.parse("2027-07-12"), RatePlan.STANDARD);
        assertEquals(2, q.nights());
        assertEquals(new BigDecimal("599.76"), q.subtotal());
        assertEquals(new BigDecimal("0.00"), q.discount());
        assertEquals(new BigDecimal("599.76"), q.total());
    }

    @Test
    void seasonBoundaries() {
        // 14/12 baixa, 15/12 alta, 28/02 alta, 01/03 baixa
        var q = Pricing.quote(standard, LocalDate.parse("2026-12-14"), LocalDate.parse("2026-12-16"), RatePlan.STANDARD);
        assertEquals(new BigDecimal("549.78"), q.subtotal());
        q = Pricing.quote(standard, LocalDate.parse("2027-02-28"), LocalDate.parse("2027-03-02"), RatePlan.STANDARD);
        assertEquals(new BigDecimal("549.78"), q.subtotal());
    }

    @Test
    void nonRefundableDiscountsFifteenPercentHalfEven() {
        var q = Pricing.quote(standard, LocalDate.parse("2027-03-10"), LocalDate.parse("2027-03-11"), RatePlan.NON_REFUNDABLE);
        assertEquals(new BigDecimal("249.90"), q.subtotal());
        assertEquals(new BigDecimal("37.48"), q.discount()); // 37.485 -> par (8)
        assertEquals(new BigDecimal("212.42"), q.total());
    }
}
