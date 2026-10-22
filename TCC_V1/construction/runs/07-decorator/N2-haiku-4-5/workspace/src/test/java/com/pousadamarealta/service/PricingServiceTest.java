package com.pousadamarealta.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class PricingServiceTest {
    private PricingService pricingService;

    @BeforeEach
    public void setUp() {
        pricingService = new PricingService();
    }

    @Test
    public void testHighSeasonMultiplier() {
        LocalDate date = LocalDate.of(2027, 7, 10); // July
        BigDecimal baseRate = new BigDecimal("100.00");
        BigDecimal price = pricingService.calculateNightlyPrice(baseRate, date);
        assertEquals(new BigDecimal("120.00"), price);
    }

    @Test
    public void testLowSeasonNoMultiplier() {
        LocalDate date = LocalDate.of(2027, 8, 10); // August
        BigDecimal baseRate = new BigDecimal("100.00");
        BigDecimal price = pricingService.calculateNightlyPrice(baseRate, date);
        assertEquals(new BigDecimal("100.00"), price);
    }

    @Test
    public void testCalculateNights() {
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);
        int nights = pricingService.calculateNights(checkIn, checkOut);
        assertEquals(2, nights);
    }

    @Test
    public void testCalculateSubtotalHighSeason() {
        BigDecimal baseRate = new BigDecimal("249.90");
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);

        BigDecimal subtotal = pricingService.calculateSubtotal(baseRate, checkIn, checkOut);
        // 249.90 * 1.2 * 2 = 599.76
        assertEquals(new BigDecimal("599.76"), subtotal);
    }

    @Test
    public void testCalculateDiscountNonRefundable() {
        BigDecimal subtotal = new BigDecimal("599.76");
        BigDecimal discount = pricingService.calculateDiscount(subtotal, "NON_REFUNDABLE");
        // 599.76 * 0.15 = 89.964 -> 89.96 (banker's rounding)
        assertEquals(new BigDecimal("89.96"), discount);
    }

    @Test
    public void testCalculateDiscountStandard() {
        BigDecimal subtotal = new BigDecimal("599.76");
        BigDecimal discount = pricingService.calculateDiscount(subtotal, "STANDARD");
        assertEquals(BigDecimal.ZERO, discount);
    }

    @Test
    public void testBankersRounding() {
        // Test rounding to 2 decimal places with HALF_EVEN
        BigDecimal value1 = new BigDecimal("1.555");
        assertEquals(new BigDecimal("1.56"), pricingService.roundBankersStyle(value1));

        BigDecimal value2 = new BigDecimal("1.545");
        assertEquals(new BigDecimal("1.54"), pricingService.roundBankersStyle(value2));

        BigDecimal value3 = new BigDecimal("89.964");
        assertEquals(new BigDecimal("89.96"), pricingService.roundBankersStyle(value3));

        BigDecimal value4 = new BigDecimal("89.965");
        assertEquals(new BigDecimal("89.96"), pricingService.roundBankersStyle(value4));
    }
}
