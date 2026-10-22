package com.pousada.maralta.service;

import com.pousada.maralta.domain.RatePlan;
import com.pousada.maralta.domain.RoomType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PricingServiceTest {
    private final PricingService pricingService = new PricingService();

    @Test
    public void testHighSeasonPricing() {
        LocalDate highSeasonDate = LocalDate.of(2027, 7, 10);
        BigDecimal price = pricingService.calculateNightPrice(RoomType.STANDARD, highSeasonDate);
        assertEquals(new BigDecimal("299.88"), price);
    }

    @Test
    public void testRegularSeasonPricing() {
        LocalDate regularDate = LocalDate.of(2027, 3, 10);
        BigDecimal price = pricingService.calculateNightPrice(RoomType.STANDARD, regularDate);
        assertEquals(new BigDecimal("249.90"), price);
    }

    @Test
    public void testSubtotalCalculation() {
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);
        BigDecimal subtotal = pricingService.calculateSubtotal(RoomType.STANDARD, checkIn, checkOut);
        assertEquals(new BigDecimal("599.76"), subtotal);
    }

    @Test
    public void testNonRefundableDiscount() {
        BigDecimal subtotal = new BigDecimal("599.76");
        BigDecimal discount = pricingService.calculateDiscount(subtotal, RatePlan.NON_REFUNDABLE);
        assertEquals(new BigDecimal("89.96"), discount);
    }

    @Test
    public void testStandardPlanNoDiscount() {
        BigDecimal subtotal = new BigDecimal("599.76");
        BigDecimal discount = pricingService.calculateDiscount(subtotal, RatePlan.STANDARD);
        assertEquals(BigDecimal.ZERO, discount);
    }

    @Test
    public void testTotalCalculation() {
        BigDecimal subtotal = new BigDecimal("599.76");
        BigDecimal discount = new BigDecimal("89.96");
        BigDecimal total = pricingService.calculateTotal(subtotal, discount);
        assertEquals(new BigDecimal("509.80"), total);
    }

    @Test
    public void testDeluxeTwoNightsRegularSeason() {
        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);
        BigDecimal subtotal = pricingService.calculateSubtotal(RoomType.DELUXE, checkIn, checkOut);
        assertEquals(new BigDecimal("779.80"), subtotal);
    }
}
