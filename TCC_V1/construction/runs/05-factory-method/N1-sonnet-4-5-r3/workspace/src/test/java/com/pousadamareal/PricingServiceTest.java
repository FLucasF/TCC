package com.pousadamareal;

import com.pousadamareal.model.RatePlan;
import com.pousadamareal.service.PricingService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PricingServiceTest {
    private final PricingService pricingService = new PricingService();

    @Test
    public void testHighSeasonPricing() {
        BigDecimal baseRate = new BigDecimal("249.90");

        LocalDate highSeasonDate = LocalDate.of(2027, 7, 10);
        BigDecimal nightPrice = pricingService.calculateNightPrice(baseRate, highSeasonDate);
        assertEquals(new BigDecimal("299.88"), nightPrice);
    }

    @Test
    public void testLowSeasonPricing() {
        BigDecimal baseRate = new BigDecimal("249.90");

        LocalDate lowSeasonDate = LocalDate.of(2027, 3, 10);
        BigDecimal nightPrice = pricingService.calculateNightPrice(baseRate, lowSeasonDate);
        assertEquals(new BigDecimal("249.90"), nightPrice);
    }

    @Test
    public void testSubtotalCalculation() {
        BigDecimal baseRate = new BigDecimal("249.90");
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);

        BigDecimal subtotal = pricingService.calculateSubtotal(baseRate, checkIn, checkOut);
        assertEquals(new BigDecimal("599.76"), subtotal);
    }

    @Test
    public void testNonRefundableDiscount() {
        BigDecimal subtotal = new BigDecimal("599.76");
        BigDecimal discount = pricingService.calculateDiscount(subtotal, RatePlan.NON_REFUNDABLE);
        assertEquals(new BigDecimal("89.96"), discount);
    }

    @Test
    public void testStandardDiscount() {
        BigDecimal subtotal = new BigDecimal("599.76");
        BigDecimal discount = pricingService.calculateDiscount(subtotal, RatePlan.STANDARD);
        assertEquals(new BigDecimal("0.00"), discount);
    }

    @Test
    public void testDecemberHighSeason() {
        BigDecimal baseRate = new BigDecimal("249.90");

        LocalDate beforeHighSeason = LocalDate.of(2027, 12, 14);
        BigDecimal nightPrice1 = pricingService.calculateNightPrice(baseRate, beforeHighSeason);
        assertEquals(new BigDecimal("249.90"), nightPrice1);

        LocalDate duringHighSeason = LocalDate.of(2027, 12, 15);
        BigDecimal nightPrice2 = pricingService.calculateNightPrice(baseRate, duringHighSeason);
        assertEquals(new BigDecimal("299.88"), nightPrice2);
    }

    @Test
    public void testJanuaryFebruaryHighSeason() {
        BigDecimal baseRate = new BigDecimal("249.90");

        LocalDate january = LocalDate.of(2027, 1, 15);
        BigDecimal nightPrice1 = pricingService.calculateNightPrice(baseRate, january);
        assertEquals(new BigDecimal("299.88"), nightPrice1);

        LocalDate february = LocalDate.of(2027, 2, 15);
        BigDecimal nightPrice2 = pricingService.calculateNightPrice(baseRate, february);
        assertEquals(new BigDecimal("299.88"), nightPrice2);
    }
}
