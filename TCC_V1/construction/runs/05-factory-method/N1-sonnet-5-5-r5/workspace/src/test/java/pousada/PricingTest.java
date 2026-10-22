package pousada;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import pousada.domain.*;

class PricingTest {
    private final Room standard = Rooms.find(101).orElseThrow();

    @Test
    void highSeasonNightsCostTwentyPercentMore() {
        var q = Pricing.quote(standard, LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 12), RatePlan.STANDARD);
        assertEquals(2, q.nights());
        assertEquals(Money.of("599.76"), q.total());
        assertEquals(Money.of("0.00"), q.discount());
    }

    @Test
    void seasonBoundaries() {
        assertFalse(Pricing.isHighSeason(LocalDate.of(2027, 12, 14)));
        assertTrue(Pricing.isHighSeason(LocalDate.of(2027, 12, 15)));
        assertTrue(Pricing.isHighSeason(LocalDate.of(2028, 2, 29)));
        assertFalse(Pricing.isHighSeason(LocalDate.of(2027, 3, 1)));
        assertTrue(Pricing.isHighSeason(LocalDate.of(2027, 7, 31)));
        assertFalse(Pricing.isHighSeason(LocalDate.of(2027, 8, 1)));
    }

    @Test
    void nonRefundableDiscountsTheSubtotal() {
        var q = Pricing.quote(standard, LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 12), RatePlan.NON_REFUNDABLE);
        assertEquals(Money.of("599.76"), q.subtotal());
        assertEquals(Money.of("89.96"), q.discount());
        assertEquals(Money.of("509.80"), q.total());
    }

    @Test
    void roundingIsHalfEven() {
        // 0.125 -> 0.12 (meio para o par), 0.135 -> 0.14
        assertEquals(Money.of("0.12"), Money.round(new java.math.BigDecimal("0.125")));
        assertEquals(Money.of("0.14"), Money.round(new java.math.BigDecimal("0.135")));
    }
}
