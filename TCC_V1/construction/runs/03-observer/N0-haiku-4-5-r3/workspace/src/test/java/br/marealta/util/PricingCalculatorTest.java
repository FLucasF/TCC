package br.marealta.util;

import br.marealta.model.RatePlan;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.Assert.*;

public class PricingCalculatorTest {

    @Test
    public void testHighSeasonDetection() {
        assertFalse(PricingCalculator.isHighSeason(LocalDate.of(2027, 3, 1)));
        assertFalse(PricingCalculator.isHighSeason(LocalDate.of(2027, 6, 15)));
        assertTrue(PricingCalculator.isHighSeason(LocalDate.of(2027, 7, 1)));
        assertTrue(PricingCalculator.isHighSeason(LocalDate.of(2027, 12, 15)));
        assertTrue(PricingCalculator.isHighSeason(LocalDate.of(2027, 1, 1)));
        assertFalse(PricingCalculator.isHighSeason(LocalDate.of(2027, 12, 14)));
    }

    @Test
    public void testNightPriceCalculation() {
        BigDecimal standardRate = new BigDecimal("249.90");

        BigDecimal lowSeasonPrice = PricingCalculator.calculateNightPrice(standardRate, LocalDate.of(2027, 3, 10));
        assertEquals("249.90", lowSeasonPrice.toString());

        BigDecimal highSeasonPrice = PricingCalculator.calculateNightPrice(standardRate, LocalDate.of(2027, 7, 10));
        assertEquals("299.88", highSeasonPrice.toString());
    }

    @Test
    public void testSubtotalCalculation() {
        BigDecimal standardRate = new BigDecimal("249.90");

        BigDecimal subtotal = PricingCalculator.calculateSubtotal(standardRate,
            LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 12));
        assertEquals("599.76", subtotal.toString());
    }

    @Test
    public void testDiscountCalculation() {
        BigDecimal subtotal = new BigDecimal("599.76");

        BigDecimal standardDiscount = PricingCalculator.calculateDiscount(subtotal, RatePlan.STANDARD);
        assertEquals(BigDecimal.ZERO, standardDiscount);

        BigDecimal nonRefundableDiscount = PricingCalculator.calculateDiscount(subtotal, RatePlan.NON_REFUNDABLE);
        assertEquals("89.96", nonRefundableDiscount.toString());
    }

    @Test
    public void testTotalCalculation() {
        BigDecimal subtotal = new BigDecimal("599.76");
        BigDecimal discount = new BigDecimal("89.96");

        BigDecimal total = PricingCalculator.calculateTotal(subtotal, discount);
        assertEquals("509.80", total.toString());
    }

    @Test
    public void testNightsCalculation() {
        int nights = PricingCalculator.calculateNights(
            LocalDate.of(2027, 7, 10),
            LocalDate.of(2027, 7, 12));
        assertEquals(2, nights);
    }
}
