package com.pousadamarealta;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class PricingServiceTest {

    @Test
    public void testStandardPriceLowSeason() {
        double nightlyPrice = PricingService.calculateNightlyPrice(249.90, LocalDate.of(2027, 3, 10));
        assertEquals(249.90, nightlyPrice);
    }

    @Test
    public void testStandardPriceHighSeason() {
        double nightlyPrice = PricingService.calculateNightlyPrice(249.90, LocalDate.of(2027, 7, 10));
        assertEquals(299.88, nightlyPrice);
    }

    @Test
    public void testSubtotalCalculation() {
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);
        double subtotal = PricingService.calculateSubtotal(RoomType.STANDARD, checkIn, checkOut);
        assertEquals(599.76, subtotal);
    }

    @Test
    public void testDiscountNonRefundable() {
        double discount = PricingService.calculateDiscount(RatePlan.NON_REFUNDABLE, 599.76);
        assertEquals(89.96, discount);
    }

    @Test
    public void testDiscountStandard() {
        double discount = PricingService.calculateDiscount(RatePlan.STANDARD, 599.76);
        assertEquals(0, discount);
    }

    @Test
    public void testRounding() {
        double value = 299.885;
        assertEquals(299.88, PricingService.round(value));
    }

    @Test
    public void testNights() {
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);
        assertEquals(2, PricingService.getNights(checkIn, checkOut));
    }
}
