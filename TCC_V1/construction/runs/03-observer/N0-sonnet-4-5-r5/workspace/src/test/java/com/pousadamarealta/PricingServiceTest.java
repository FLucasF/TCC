package com.pousadamarealta;

import com.pousadamarealta.domain.RatePlan;
import com.pousadamarealta.domain.RoomType;
import com.pousadamarealta.service.PricingService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PricingServiceTest {
    private final PricingService pricingService = new PricingService();

    @Test
    void testNormalSeasonPricing() {
        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        BigDecimal subtotal = pricingService.calculateSubtotal(RoomType.STANDARD, checkIn, checkOut);

        assertEquals(new BigDecimal("499.80"), subtotal);
    }

    @Test
    void testHighSeasonPricing() {
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);

        BigDecimal subtotal = pricingService.calculateSubtotal(RoomType.STANDARD, checkIn, checkOut);

        BigDecimal expectedNightlyRate = new BigDecimal("249.90").multiply(new BigDecimal("1.20"));
        BigDecimal expected = expectedNightlyRate.multiply(new BigDecimal("2")).setScale(2, java.math.RoundingMode.HALF_EVEN);

        assertEquals(new BigDecimal("599.76"), subtotal);
    }

    @Test
    void testNonRefundableDiscount() {
        BigDecimal subtotal = new BigDecimal("500.00");
        BigDecimal discount = pricingService.calculateDiscount(subtotal, RatePlan.NON_REFUNDABLE);

        assertEquals(new BigDecimal("75.00"), discount);
    }

    @Test
    void testStandardNoDiscount() {
        BigDecimal subtotal = new BigDecimal("500.00");
        BigDecimal discount = pricingService.calculateDiscount(subtotal, RatePlan.STANDARD);

        assertEquals(BigDecimal.ZERO, discount);
    }

    @Test
    void testDecemberHighSeason() {
        LocalDate checkIn = LocalDate.of(2027, 12, 15);
        LocalDate checkOut = LocalDate.of(2027, 12, 16);

        BigDecimal subtotal = pricingService.calculateSubtotal(RoomType.DELUXE, checkIn, checkOut);

        BigDecimal expectedNightlyRate = new BigDecimal("389.90").multiply(new BigDecimal("1.20"))
                .setScale(2, java.math.RoundingMode.HALF_EVEN);

        assertEquals(new BigDecimal("467.88"), subtotal);
    }

    @Test
    void testJanuaryHighSeason() {
        LocalDate checkIn = LocalDate.of(2027, 1, 10);
        LocalDate checkOut = LocalDate.of(2027, 1, 11);

        BigDecimal subtotal = pricingService.calculateSubtotal(RoomType.SUITE, checkIn, checkOut);

        BigDecimal expectedNightlyRate = new BigDecimal("579.90").multiply(new BigDecimal("1.20"))
                .setScale(2, java.math.RoundingMode.HALF_EVEN);

        assertEquals(new BigDecimal("695.88"), subtotal);
    }
}
