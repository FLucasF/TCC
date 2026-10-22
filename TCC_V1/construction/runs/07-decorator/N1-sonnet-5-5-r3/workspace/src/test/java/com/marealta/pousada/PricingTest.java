package com.marealta.pousada;

import com.marealta.pousada.domain.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PricingTest {
    @Test
    void highSeasonNights() {
        Quote q = Quote.of(RoomType.STANDARD, RatePlan.STANDARD, LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 12));
        assertEquals(new BigDecimal("599.76"), q.total());
        assertEquals(2, q.nights());
    }

    @Test
    void nonRefundableDiscountRoundsHalfEven() {
        Quote q = Quote.of(RoomType.SUITE, RatePlan.NON_REFUNDABLE, LocalDate.of(2027, 3, 1), LocalDate.of(2027, 3, 2));
        assertEquals(new BigDecimal("579.90"), q.subtotal());
        assertEquals(new BigDecimal("86.98"), q.discount()); // 86.985 -> 86.98 (par)
        assertEquals(new BigDecimal("492.92"), q.total());
    }

    @Test
    void seasonBoundaries() {
        assertFalse(Season.isHigh(LocalDate.of(2027, 12, 14)));
        assertTrue(Season.isHigh(LocalDate.of(2027, 12, 15)));
        assertTrue(Season.isHigh(LocalDate.of(2028, 2, 29)));
        assertFalse(Season.isHigh(LocalDate.of(2028, 3, 1)));
    }

    @Test
    void moneyFormat() {
        assertEquals("R$ 1.234,56", Money.format(new BigDecimal("1234.56")));
    }
}
