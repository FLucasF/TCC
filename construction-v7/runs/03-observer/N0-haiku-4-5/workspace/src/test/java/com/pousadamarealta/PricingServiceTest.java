package com.pousadamarealta;

import com.pousadamarealta.service.PricingService;
import org.junit.Test;
import static org.junit.Assert.*;
import java.time.LocalDate;

public class PricingServiceTest {

    @Test
    public void testHighSeasonJuly() {
        assertTrue(PricingService.isHighSeason(LocalDate.of(2027, 7, 10)));
    }

    @Test
    public void testHighSeasonDecember() {
        assertTrue(PricingService.isHighSeason(LocalDate.of(2027, 12, 25)));
    }

    @Test
    public void testHighSeasonJanuary() {
        assertTrue(PricingService.isHighSeason(LocalDate.of(2027, 1, 15)));
    }

    @Test
    public void testHighSeasonFebruary() {
        assertTrue(PricingService.isHighSeason(LocalDate.of(2027, 2, 28)));
    }

    @Test
    public void testLowSeason() {
        assertFalse(PricingService.isHighSeason(LocalDate.of(2027, 3, 10)));
        assertFalse(PricingService.isHighSeason(LocalDate.of(2027, 6, 10)));
    }

    @Test
    public void testRoundHalfToEven() {
        assertEquals(249.90, PricingService.roundHalfToEven(249.90), 0.001);
        assertEquals(299.88, PricingService.roundHalfToEven(299.88), 0.001);
    }

    @Test
    public void testCalculateNightlyPriceHighSeason() {
        double price = PricingService.calculateNightlyPrice(249.90, LocalDate.of(2027, 7, 10));
        assertEquals(299.88, price, 0.01);
    }

    @Test
    public void testCalculateNightlyPriceLowSeason() {
        double price = PricingService.calculateNightlyPrice(249.90, LocalDate.of(2027, 3, 10));
        assertEquals(249.90, price, 0.01);
    }

    @Test
    public void testCalculateSubtotal() {
        double[] prices = {299.88, 299.88};
        double subtotal = PricingService.calculateSubtotal(prices);
        assertEquals(599.76, subtotal, 0.01);
    }

    @Test
    public void testCalculateDiscount() {
        double discount = PricingService.calculateDiscount(599.76, "NON_REFUNDABLE");
        assertEquals(89.964, discount, 0.01);
    }

    @Test
    public void testCalculateNoPriceDiscount() {
        double discount = PricingService.calculateDiscount(599.76, "STANDARD");
        assertEquals(0.0, discount, 0.01);
    }
}
