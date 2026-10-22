package br.com.maretalta;

import br.com.maretalta.domain.Money;
import br.com.maretalta.domain.Pricing;
import br.com.maretalta.domain.RatePlan;
import br.com.maretalta.domain.RoomType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class PricingTest {
    @Test
    void highSeasonBoundaries() {
        assertFalse(Pricing.highSeason(LocalDate.of(2027, 12, 14)));
        assertTrue(Pricing.highSeason(LocalDate.of(2027, 12, 15)));
        assertTrue(Pricing.highSeason(LocalDate.of(2027, 2, 28)));
        assertFalse(Pricing.highSeason(LocalDate.of(2027, 3, 1)));
        assertTrue(Pricing.highSeason(LocalDate.of(2027, 7, 31)));
        assertFalse(Pricing.highSeason(LocalDate.of(2027, 8, 1)));
    }

    @Test
    void standardJulyExample() {
        var q = Pricing.quote(RoomType.STANDARD, LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 12), RatePlan.STANDARD);
        assertEquals(2, q.nights());
        assertEquals(Money.of("599.76"), q.total());
        assertEquals(Money.of("0.00"), q.discount());
    }

    @Test
    void nonRefundableDiscountAndMixedSeason() {
        // noites de 30/06 (baixa) e 01/07 (alta): 249.90 + 299.88 = 549.78; 15% = 82.467 -> 82.47
        var q = Pricing.quote(RoomType.STANDARD, LocalDate.of(2027, 6, 30), LocalDate.of(2027, 7, 2), RatePlan.NON_REFUNDABLE);
        assertEquals(Money.of("549.78"), q.subtotal());
        assertEquals(Money.of("82.47"), q.discount());
        assertEquals(Money.of("467.31"), q.total());
    }

    @Test
    void halfEvenRounding() {
        assertEquals(Money.of("0.12"), Money.round(new java.math.BigDecimal("0.125")));
        assertEquals(Money.of("0.14"), Money.round(new java.math.BigDecimal("0.135")));
    }

    @Test
    void brlFormat() {
        assertEquals("R$ 1.234,56", Money.brl(Money.of("1234.56")));
        assertEquals("R$ 0,00", Money.brl(Money.ZERO));
        assertEquals("R$ 1.000.000,10", Money.brl(Money.of("1000000.1")));
    }
}
