package com.pousadamarealta.integration;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import com.pousadamarealta.models.RatePlan;
import com.pousadamarealta.util.PricingCalculator;
import static org.junit.jupiter.api.Assertions.*;

public class PricingIntegrationTest {
    @Test
    public void testStandardRoomHighSeason() {
        BigDecimal baseRate = new BigDecimal("249.90");
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);

        int nights = PricingCalculator.calculateNights(checkIn, checkOut);
        assertEquals(2, nights);

        BigDecimal subtotal = BigDecimal.ZERO;
        for (int i = 0; i < nights; i++) {
            LocalDate nightDate = checkIn.plusDays(i);
            BigDecimal rate = PricingCalculator.calculateNightlyRate(baseRate, nightDate);
            subtotal = subtotal.add(rate);
        }
        subtotal = com.pousadamarealta.util.PriceRounding.round(subtotal);

        assertEquals(new BigDecimal("599.76"), subtotal);

        BigDecimal discount = PricingCalculator.calculateDiscount(subtotal, RatePlan.STANDARD);
        assertEquals(BigDecimal.ZERO, discount);

        BigDecimal total = PricingCalculator.calculateTotal(subtotal, discount);
        assertEquals(new BigDecimal("599.76"), total);
    }

    @Test
    public void testStandardRoomWithNonRefundableDiscount() {
        BigDecimal baseRate = new BigDecimal("249.90");
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);

        int nights = PricingCalculator.calculateNights(checkIn, checkOut);
        BigDecimal subtotal = BigDecimal.ZERO;
        for (int i = 0; i < nights; i++) {
            LocalDate nightDate = checkIn.plusDays(i);
            BigDecimal rate = PricingCalculator.calculateNightlyRate(baseRate, nightDate);
            subtotal = subtotal.add(rate);
        }
        subtotal = com.pousadamarealta.util.PriceRounding.round(subtotal);

        BigDecimal discount = PricingCalculator.calculateDiscount(subtotal, RatePlan.NON_REFUNDABLE);
        assertEquals(new BigDecimal("89.96"), discount);

        BigDecimal total = PricingCalculator.calculateTotal(subtotal, discount);
        assertEquals(new BigDecimal("509.80"), total);
    }
}
