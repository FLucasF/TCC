package br.com.pousadamareal.service;

import br.com.pousadamareal.domain.RatePlan;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PricingServiceTest {
    private final PricingService pricingService = new PricingService();

    @Test
    void testLowSeasonStandardRate() {
        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        var result = pricingService.calculatePrice(checkIn, checkOut, 249.90, RatePlan.STANDARD);

        assertEquals(2, result.nights);
        assertEquals(249.90, result.nightlyRate);
        assertEquals(499.80, result.subtotal);
        assertEquals(0, result.discount);
        assertEquals(499.80, result.total);
    }

    @Test
    void testHighSeasonMultiplier() {
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);

        var result = pricingService.calculatePrice(checkIn, checkOut, 249.90, RatePlan.STANDARD);

        assertEquals(2, result.nights);
        assertEquals(299.88, result.nightlyRate);
        assertEquals(599.76, result.subtotal);
        assertEquals(0, result.discount);
        assertEquals(599.76, result.total);
    }

    @Test
    void testNonRefundableDiscount() {
        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        var result = pricingService.calculatePrice(checkIn, checkOut, 249.90, RatePlan.NON_REFUNDABLE);

        assertEquals(2, result.nights);
        assertEquals(249.90, result.nightlyRate);
        assertEquals(499.80, result.subtotal);
        assertEquals(74.97, result.discount);
        assertEquals(424.83, result.total);
    }

    @Test
    void testDeluxeRoom() {
        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        var result = pricingService.calculatePrice(checkIn, checkOut, 389.90, RatePlan.STANDARD);

        assertEquals(2, result.nights);
        assertEquals(389.90, result.nightlyRate);
        assertEquals(779.80, result.subtotal);
        assertEquals(0, result.discount);
        assertEquals(779.80, result.total);
    }
}
