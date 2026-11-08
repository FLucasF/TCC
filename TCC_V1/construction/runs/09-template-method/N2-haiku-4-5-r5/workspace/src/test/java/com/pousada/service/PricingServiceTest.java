package com.pousada.service;

import com.pousada.domain.Money;
import com.pousada.domain.RatePlan;
import com.pousada.domain.Room;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PricingServiceTest {
    @Test
    void testNormalSeasonPricing() {
        Room room = new Room(101, "STANDARD", 2, new BigDecimal("249.90"));
        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        PricingService.PricingResult result = PricingService.calculatePrice(room, checkIn, checkOut, RatePlan.STANDARD);

        assertEquals(2, result.nights);
        assertEquals(new BigDecimal("499.80"), result.subtotal);
        assertEquals(BigDecimal.ZERO, result.discount);
        assertEquals(new BigDecimal("499.80"), result.total);
    }

    @Test
    void testHighSeasonPricing() {
        Room room = new Room(101, "STANDARD", 2, new BigDecimal("249.90"));
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);

        PricingService.PricingResult result = PricingService.calculatePrice(room, checkIn, checkOut, RatePlan.STANDARD);

        assertEquals(2, result.nights);
        BigDecimal expectedNightlyRate = Money.multiply(new BigDecimal("249.90"), new BigDecimal("1.20"));
        BigDecimal expectedSubtotal = Money.round(expectedNightlyRate.multiply(new BigDecimal(2)));
        assertEquals(expectedSubtotal, result.subtotal);
        assertEquals(BigDecimal.ZERO, result.discount);
    }

    @Test
    void testNonRefundableDiscount() {
        Room room = new Room(101, "STANDARD", 2, new BigDecimal("249.90"));
        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        PricingService.PricingResult result = PricingService.calculatePrice(room, checkIn, checkOut, RatePlan.NON_REFUNDABLE);

        assertEquals(2, result.nights);
        assertEquals(new BigDecimal("499.80"), result.subtotal);
        assertEquals(new BigDecimal("74.97"), result.discount);
        assertEquals(new BigDecimal("424.83"), result.total);
    }

    @Test
    void testHighSeasonWithNonRefundable() {
        Room room = new Room(101, "STANDARD", 2, new BigDecimal("249.90"));
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);

        PricingService.PricingResult result = PricingService.calculatePrice(room, checkIn, checkOut, RatePlan.NON_REFUNDABLE);

        assertEquals(2, result.nights);
        BigDecimal expectedNightlyRate = Money.multiply(new BigDecimal("249.90"), new BigDecimal("1.20"));
        BigDecimal expectedSubtotal = Money.round(expectedNightlyRate.multiply(new BigDecimal(2)));
        assertEquals(expectedSubtotal, result.subtotal);
        BigDecimal expectedDiscount = Money.percent(expectedSubtotal, 15);
        assertEquals(expectedDiscount, result.discount);
    }
}
