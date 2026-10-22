package com.pousadamarealta.service;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class PricingServiceTest {

    @Test
    public void testNormalSeasonStandardRate() {
        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);
        BigDecimal baseRate = new BigDecimal("249.90");

        PricingService.PriceCalculation price = PricingService.calculatePrice(baseRate, checkIn, checkOut, "STANDARD");

        assertEquals(0, price.subtotal.compareTo(new BigDecimal("499.80")));
        assertEquals(0, price.discount.compareTo(new BigDecimal("0.00")));
        assertEquals(0, price.total.compareTo(new BigDecimal("499.80")));
    }

    @Test
    public void testHighSeasonJuly() {
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);
        BigDecimal baseRate = new BigDecimal("249.90");

        PricingService.PriceCalculation price = PricingService.calculatePrice(baseRate, checkIn, checkOut, "STANDARD");

        BigDecimal expectedNightPrice = new BigDecimal("299.88");
        assertEquals(0, price.subtotal.compareTo(expectedNightPrice.multiply(new BigDecimal("2"))));
    }

    @Test
    public void testNonRefundableDiscount() {
        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);
        BigDecimal baseRate = new BigDecimal("249.90");

        PricingService.PriceCalculation price = PricingService.calculatePrice(baseRate, checkIn, checkOut, "NON_REFUNDABLE");

        assertEquals(0, price.discount.compareTo(new BigDecimal("74.97")));
    }

    @Test
    public void testCalculateNights() {
        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        int nights = PricingService.calculateNights(checkIn, checkOut);
        assertEquals(2, nights);
    }
}
