package br.com.marealta;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class PricingTest {
    private final Room standard = Room.of(101, RoomType.STANDARD);

    @Test
    void highSeasonNightsCostTwentyPercentMore() {
        var q = Pricing.quote(standard, LocalDate.parse("2027-07-10"), LocalDate.parse("2027-07-12"), RatePlan.STANDARD);
        assertEquals(2, q.nights());
        assertEquals(Money.of("599.76"), q.subtotal());
        assertEquals(Money.of("599.76"), q.total());
    }

    @Test
    void seasonBoundaries() {
        assertFalse(Pricing.isHighSeason(LocalDate.parse("2027-12-14")));
        assertTrue(Pricing.isHighSeason(LocalDate.parse("2027-12-15")));
        assertTrue(Pricing.isHighSeason(LocalDate.parse("2028-02-29")));
        assertFalse(Pricing.isHighSeason(LocalDate.parse("2028-03-01")));
        assertFalse(Pricing.isHighSeason(LocalDate.parse("2027-06-30")));
        assertTrue(Pricing.isHighSeason(LocalDate.parse("2027-07-31")));
        assertFalse(Pricing.isHighSeason(LocalDate.parse("2027-08-01")));
    }

    @Test
    void nonRefundableGivesFifteenPercentOffTheSubtotal() {
        var q = Pricing.quote(standard, LocalDate.parse("2027-03-10"), LocalDate.parse("2027-03-12"), RatePlan.NON_REFUNDABLE);
        assertEquals(Money.of("499.80"), q.subtotal());
        assertEquals(Money.of("74.97"), q.discount());
        assertEquals(Money.of("424.83"), q.total());
    }

    @Test
    void discountRoundsHalfToEven() {
        // 0,15 * 0,30 = 0,045 -> 0,04 (par), e 0,15 * 0,10 = 0,015 -> 0,02
        assertEquals(Money.of("0.04"), RatePlan.NON_REFUNDABLE.discountOn(Money.of("0.30")));
        assertEquals(Money.of("0.02"), RatePlan.NON_REFUNDABLE.discountOn(Money.of("0.10")));
    }
}
