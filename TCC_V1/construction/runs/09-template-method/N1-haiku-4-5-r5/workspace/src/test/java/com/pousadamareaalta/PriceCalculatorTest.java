package com.pousadamareaalta;

import com.pousadamareaalta.util.PriceCalculator;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class PriceCalculatorTest {
    @Test
    public void testHighSeasonDetection() {
        assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2027, 7, 10)));
        assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2027, 12, 15)));
        assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2027, 12, 31)));
        assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2027, 1, 1)));
        assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2027, 2, 28)));

        assertFalse(PriceCalculator.isHighSeason(LocalDate.of(2027, 12, 14)));
        assertFalse(PriceCalculator.isHighSeason(LocalDate.of(2027, 3, 1)));
        assertFalse(PriceCalculator.isHighSeason(LocalDate.of(2027, 6, 1)));
    }

    @Test
    public void testNightlyPriceCalculation() {
        double baseRate = 249.90;

        double lowSeasonPrice = PriceCalculator.calculateNightlyPrice(baseRate, LocalDate.of(2027, 3, 1));
        assertEquals(249.90, lowSeasonPrice);

        double highSeasonPrice = PriceCalculator.calculateNightlyPrice(baseRate, LocalDate.of(2027, 7, 10));
        assertEquals(299.88, highSeasonPrice);
    }

    @Test
    public void testSubtotalCalculation() {
        double baseRate = 249.90;
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);

        double subtotal = PriceCalculator.calculateSubtotal(baseRate, checkIn, checkOut);
        assertEquals(599.76, subtotal);
    }

    @Test
    public void testDiscountCalculation() {
        double subtotal = 599.76;

        double standardDiscount = PriceCalculator.calculateDiscount(subtotal, "STANDARD");
        assertEquals(0.0, standardDiscount);

        double nonRefundableDiscount = PriceCalculator.calculateDiscount(subtotal, "NON_REFUNDABLE");
        assertEquals(89.96, nonRefundableDiscount, 0.01);
    }

    @Test
    public void testTotalCalculation() {
        double subtotal = 599.76;
        double discount = 89.96;

        double total = PriceCalculator.calculateTotal(subtotal, discount);
        assertEquals(509.80, total, 0.01);
    }
}
