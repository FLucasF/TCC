package com.pousadamarealta;

import com.pousadamarealta.domain.RatePlan;
import com.pousadamarealta.domain.Room;
import com.pousadamarealta.domain.RoomType;
import com.pousadamarealta.service.PricingService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class PricingServiceTest {

    @Test
    void testLowSeasonPricing() {
        PricingService pricingService = new PricingService();
        Room room = new Room("101", RoomType.STANDARD, 2, new BigDecimal("249.90"));

        BigDecimal rate = pricingService.calculateNightlyRate(room, LocalDate.of(2027, 3, 10));
        assertEquals(new BigDecimal("249.90"), rate);
    }

    @Test
    void testHighSeasonPricing() {
        PricingService pricingService = new PricingService();
        Room room = new Room("101", RoomType.STANDARD, 2, new BigDecimal("249.90"));

        BigDecimal rate = pricingService.calculateNightlyRate(room, LocalDate.of(2027, 7, 10));
        assertEquals(new BigDecimal("299.88"), rate);
    }

    @Test
    void testNonRefundableDiscount() {
        PricingService pricingService = new PricingService();
        BigDecimal subtotal = new BigDecimal("599.76");

        BigDecimal discount = pricingService.calculateDiscount(subtotal, RatePlan.NON_REFUNDABLE);
        assertEquals(new BigDecimal("89.96"), discount);
    }

    @Test
    void testStandardPlanNoDiscount() {
        PricingService pricingService = new PricingService();
        BigDecimal subtotal = new BigDecimal("599.76");

        BigDecimal discount = pricingService.calculateDiscount(subtotal, RatePlan.STANDARD);
        assertEquals(new BigDecimal("0.00"), discount);
    }
}
