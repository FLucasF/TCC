package com.pousadamarealta.service;

import com.pousadamarealta.model.RoomType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class PricingServiceTest {
    private final PricingService pricingService = new PricingService();

    @Test
    void testRegularSeasonPricing() {
        LocalDate date = LocalDate.of(2027, 3, 10);
        BigDecimal rate = pricingService.calculateNightlyRate(RoomType.STANDARD, date);
        assertEquals(new BigDecimal("249.90"), rate);
    }

    @Test
    void testHighSeasonPricingJuly() {
        LocalDate date = LocalDate.of(2027, 7, 10);
        BigDecimal rate = pricingService.calculateNightlyRate(RoomType.STANDARD, date);
        assertEquals(new BigDecimal("299.88"), rate);
    }

    @Test
    void testHighSeasonPricingDecember() {
        LocalDate date = LocalDate.of(2027, 12, 20);
        BigDecimal rate = pricingService.calculateNightlyRate(RoomType.STANDARD, date);
        assertEquals(new BigDecimal("299.88"), rate);
    }

    @Test
    void testHighSeasonPricingJanuary() {
        LocalDate date = LocalDate.of(2027, 1, 15);
        BigDecimal rate = pricingService.calculateNightlyRate(RoomType.STANDARD, date);
        assertEquals(new BigDecimal("299.88"), rate);
    }

    @Test
    void testHighSeasonPricingFebruary() {
        LocalDate date = LocalDate.of(2027, 2, 10);
        BigDecimal rate = pricingService.calculateNightlyRate(RoomType.STANDARD, date);
        assertEquals(new BigDecimal("299.88"), rate);
    }

    @Test
    void testNotHighSeasonDecemberBefore15() {
        LocalDate date = LocalDate.of(2027, 12, 14);
        BigDecimal rate = pricingService.calculateNightlyRate(RoomType.STANDARD, date);
        assertEquals(new BigDecimal("249.90"), rate);
    }

    @Test
    void testRounding() {
        BigDecimal value = new BigDecimal("10.125");
        BigDecimal rounded = pricingService.round(value);
        assertEquals(new BigDecimal("10.12"), rounded);

        BigDecimal value2 = new BigDecimal("10.135");
        BigDecimal rounded2 = pricingService.round(value2);
        assertEquals(new BigDecimal("10.14"), rounded2);
    }
}
