package br.com.marealta;

import br.com.marealta.reservation.Pricing;
import br.com.marealta.reservation.RatePlan;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PricingTest {

    @Test
    void highSeasonBoundaries() {
        assertFalse(Pricing.isHighSeason(LocalDate.of(2027, 12, 14)));
        assertTrue(Pricing.isHighSeason(LocalDate.of(2027, 12, 15)));
        assertTrue(Pricing.isHighSeason(LocalDate.of(2028, 2, 29)));
        assertFalse(Pricing.isHighSeason(LocalDate.of(2027, 3, 1)));
        assertTrue(Pricing.isHighSeason(LocalDate.of(2027, 7, 31)));
        assertFalse(Pricing.isHighSeason(LocalDate.of(2027, 8, 1)));
    }

    @Test
    void highSeasonStandard() {
        Pricing.Quote q = Pricing.quote(new BigDecimal("249.90"), LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 12),
                RatePlan.STANDARD);
        assertEquals(2, q.nights());
        assertEquals(new BigDecimal("599.76"), q.total());
        assertEquals(new BigDecimal("0.00"), q.discount());
    }

    @Test
    void nonRefundableDiscountsSubtotal() {
        Pricing.Quote q = Pricing.quote(new BigDecimal("389.90"), LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
                RatePlan.NON_REFUNDABLE);
        assertEquals(new BigDecimal("779.80"), q.subtotal());
        assertEquals(new BigDecimal("116.97"), q.discount());
        assertEquals(new BigDecimal("662.83"), q.total());
    }

    @Test
    void stayCrossingSeasonPricesEachNight() {
        Pricing.Quote q = Pricing.quote(new BigDecimal("249.90"), LocalDate.of(2027, 12, 14), LocalDate.of(2027, 12, 16),
                RatePlan.STANDARD);
        assertEquals(new BigDecimal("549.78"), q.subtotal());
    }

    @Test
    void halfEvenRounding() {
        assertEquals(new BigDecimal("0.12"), Money.round(new BigDecimal("0.125")));
        assertEquals(new BigDecimal("0.14"), Money.round(new BigDecimal("0.135")));
        assertEquals("R$ 1.234,56", Money.brl(new BigDecimal("1234.56")));
    }
}
