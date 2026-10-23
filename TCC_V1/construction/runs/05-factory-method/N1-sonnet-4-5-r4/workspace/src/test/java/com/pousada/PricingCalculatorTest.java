package com.pousada;

import com.pousada.domain.PricingCalculator;
import com.pousada.domain.RatePlan;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PricingCalculatorTest {

    @Test
    void testHighSeasonDetection() {
        assertTrue(PricingCalculator.isHighSeason(LocalDate.of(2027, 7, 10)));
        assertTrue(PricingCalculator.isHighSeason(LocalDate.of(2027, 12, 15)));
        assertTrue(PricingCalculator.isHighSeason(LocalDate.of(2027, 12, 31)));
        assertTrue(PricingCalculator.isHighSeason(LocalDate.of(2027, 1, 1)));
        assertTrue(PricingCalculator.isHighSeason(LocalDate.of(2027, 2, 28)));
    }

    @Test
    void testStandardRoomHighSeason() {
        BigDecimal baseRate = new BigDecimal("249.90");
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);

        BigDecimal subtotal = PricingCalculator.calculateSubtotal(baseRate, checkIn, checkOut);
        assertEquals(new BigDecimal("599.76"), subtotal);
    }

    @Test
    void testNonRefundableDiscount() {
        BigDecimal subtotal = new BigDecimal("599.76");
        BigDecimal discount = PricingCalculator.calculateDiscount(subtotal, RatePlan.NON_REFUNDABLE);
        assertEquals(new BigDecimal("89.96"), discount);
    }
}
