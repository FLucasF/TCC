package br.com.marealta;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class PricingTest {
    Room standard = ReservationService.ROOMS.get(0);

    @Test
    void highSeasonExample() {
        var q = Pricing.quote(standard, LocalDate.parse("2027-07-10"), LocalDate.parse("2027-07-12"), RatePlan.STANDARD);
        assertEquals(2, q.nights());
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
    void nonRefundableDiscountAndRounding() {
        var q = Pricing.quote(ReservationService.ROOMS.get(2), LocalDate.parse("2027-03-10"),
                LocalDate.parse("2027-03-12"), RatePlan.NON_REFUNDABLE);
        assertEquals(Money.of("779.80"), q.subtotal());
        assertEquals(Money.of("116.97"), q.discount()); // 116.97 exato
        assertEquals(Money.of("662.83"), q.total());
    }

    @Test
    void moneyFormat() {
        assertEquals("R$ 1.234,56", Money.brl(Money.of("1234.56")));
        assertEquals("R$ 0,00", Money.brl(Money.of("0")));
        assertEquals(Money.of("0.12"), Money.round(new java.math.BigDecimal("0.125")));
        assertEquals(Money.of("0.14"), Money.round(new java.math.BigDecimal("0.135")));
    }
}
