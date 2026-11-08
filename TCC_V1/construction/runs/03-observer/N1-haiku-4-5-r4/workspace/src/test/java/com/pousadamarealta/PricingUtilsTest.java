package com.pousadamarealta;

import com.pousadamarealta.utils.PricingUtils;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class PricingUtilsTest {

    @Test
    public void testHighSeasonMonth7() {
        assertTrue(PricingUtils.isHighSeason(LocalDate.of(2027, 7, 10)));
    }

    @Test
    public void testHighSeasonDecember() {
        assertTrue(PricingUtils.isHighSeason(LocalDate.of(2027, 12, 15)));
        assertTrue(PricingUtils.isHighSeason(LocalDate.of(2027, 12, 31)));
        assertFalse(PricingUtils.isHighSeason(LocalDate.of(2027, 12, 14)));
    }

    @Test
    public void testHighSeasonJanFeb() {
        assertTrue(PricingUtils.isHighSeason(LocalDate.of(2027, 1, 1)));
        assertTrue(PricingUtils.isHighSeason(LocalDate.of(2027, 2, 28)));
    }

    @Test
    public void testLowSeason() {
        assertFalse(PricingUtils.isHighSeason(LocalDate.of(2027, 3, 15)));
        assertFalse(PricingUtils.isHighSeason(LocalDate.of(2027, 6, 30)));
    }

    @Test
    public void testNightPrice() {
        BigDecimal rate = new BigDecimal("249.90");
        BigDecimal lowSeasonPrice = PricingUtils.calculateNightPrice(rate, LocalDate.of(2027, 3, 15));
        assertEquals(new BigDecimal("249.90"), lowSeasonPrice);

        BigDecimal highSeasonPrice = PricingUtils.calculateNightPrice(rate, LocalDate.of(2027, 7, 10));
        assertEquals(new BigDecimal("299.88"), highSeasonPrice);
    }

    @Test
    public void testCalculateSubtotal() {
        BigDecimal rate = new BigDecimal("249.90");
        BigDecimal subtotal = PricingUtils.calculateSubtotal(
            rate,
            LocalDate.of(2027, 7, 10),
            LocalDate.of(2027, 7, 12)
        );
        assertEquals(new BigDecimal("599.76"), subtotal);
    }
}
