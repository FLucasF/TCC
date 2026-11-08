package com.pousadamarealta.service;

import com.pousadamarealta.domain.RatePlan;
import com.pousadamarealta.domain.Room;
import com.pousadamarealta.domain.RoomType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PricingServiceTest {
    private PricingService pricingService;
    private Room standardRoom;

    @BeforeEach
    void setUp() {
        pricingService = new PricingService();
        standardRoom = new Room("101", RoomType.STANDARD);
    }

    @Test
    void testNormalSeasonRate() {
        LocalDate date = LocalDate.of(2027, 3, 10);
        BigDecimal rate = pricingService.calculateNightlyRate(standardRoom, date);
        assertEquals(new BigDecimal("249.90"), rate);
    }

    @Test
    void testHighSeasonRateInJuly() {
        LocalDate date = LocalDate.of(2027, 7, 10);
        BigDecimal rate = pricingService.calculateNightlyRate(standardRoom, date);
        assertEquals(new BigDecimal("299.88"), rate);
    }

    @Test
    void testHighSeasonRateInDecember() {
        LocalDate date = LocalDate.of(2027, 12, 20);
        BigDecimal rate = pricingService.calculateNightlyRate(standardRoom, date);
        assertEquals(new BigDecimal("299.88"), rate);
    }

    @Test
    void testHighSeasonRateInJanuary() {
        LocalDate date = LocalDate.of(2027, 1, 15);
        BigDecimal rate = pricingService.calculateNightlyRate(standardRoom, date);
        assertEquals(new BigDecimal("299.88"), rate);
    }

    @Test
    void testSubtotalCalculation() {
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);
        BigDecimal subtotal = pricingService.calculateSubtotal(standardRoom, checkIn, checkOut);
        assertEquals(new BigDecimal("599.76"), subtotal);
    }

    @Test
    void testNonRefundableDiscount() {
        BigDecimal subtotal = new BigDecimal("599.76");
        BigDecimal discount = pricingService.calculateDiscount(subtotal, RatePlan.NON_REFUNDABLE);
        assertEquals(new BigDecimal("89.96"), discount);
    }

    @Test
    void testStandardPlanNoDiscount() {
        BigDecimal subtotal = new BigDecimal("599.76");
        BigDecimal discount = pricingService.calculateDiscount(subtotal, RatePlan.STANDARD);
        assertEquals(BigDecimal.ZERO, discount);
    }

    @Test
    void testTotalCalculation() {
        BigDecimal subtotal = new BigDecimal("599.76");
        BigDecimal discount = new BigDecimal("89.96");
        BigDecimal total = pricingService.calculateTotal(subtotal, discount);
        assertEquals(new BigDecimal("509.80"), total);
    }
}
