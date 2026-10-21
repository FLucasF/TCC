package com.pousadamarealta;

import com.pousadamarealta.domain.RatePlan;
import com.pousadamarealta.service.PricingService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class PricingServiceTest {
    private final PricingService pricingService = new PricingService();

    @Test
    void testHighSeasonPricing() {
        BigDecimal baseRate = new BigDecimal("249.90");
        LocalDate highSeasonDate = LocalDate.of(2027, 7, 10);

        BigDecimal nightPrice = pricingService.calculateNightPrice(baseRate, highSeasonDate);

        assertEquals(new BigDecimal("299.88"), nightPrice);
    }

    @Test
    void testRegularSeasonPricing() {
        BigDecimal baseRate = new BigDecimal("249.90");
        LocalDate regularDate = LocalDate.of(2027, 3, 10);

        BigDecimal nightPrice = pricingService.calculateNightPrice(baseRate, regularDate);

        assertEquals(new BigDecimal("249.90"), nightPrice);
    }

    @Test
    void testSubtotalCalculation() {
        BigDecimal baseRate = new BigDecimal("249.90");
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);

        BigDecimal subtotal = pricingService.calculateSubtotal(baseRate, checkIn, checkOut);

        assertEquals(new BigDecimal("599.76"), subtotal);
    }

    @Test
    void testNonRefundableDiscount() {
        BigDecimal subtotal = new BigDecimal("599.76");

        BigDecimal discount = pricingService.calculateDiscount(subtotal, RatePlan.NON_REFUNDABLE);

        assertEquals(new BigDecimal("89.96"), discount);
    }

    @Test
    void testStandardNoDiscount() {
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
