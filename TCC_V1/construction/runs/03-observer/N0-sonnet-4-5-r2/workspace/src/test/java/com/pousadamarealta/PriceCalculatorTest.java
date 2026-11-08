package com.pousadamarealta;

import com.pousadamarealta.util.PriceCalculator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class PriceCalculatorTest {

    @Test
    public void testRoundMoney() {
        assertEquals(new BigDecimal("10.50"),
            PriceCalculator.roundMoney(new BigDecimal("10.505")));
        assertEquals(new BigDecimal("10.52"),
            PriceCalculator.roundMoney(new BigDecimal("10.515")));
    }

    @Test
    public void testHighSeasonJuly() {
        assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2027, 7, 1)));
        assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2027, 7, 15)));
        assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2027, 7, 31)));
    }

    @Test
    public void testHighSeasonDecemberToFebruary() {
        assertFalse(PriceCalculator.isHighSeason(LocalDate.of(2027, 12, 14)));
        assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2027, 12, 15)));
        assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2027, 12, 31)));
        assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2028, 1, 15)));
        assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2028, 2, 28)));
        assertFalse(PriceCalculator.isHighSeason(LocalDate.of(2028, 3, 1)));
    }

    @Test
    public void testCalculateNightPriceNormalSeason() {
        BigDecimal baseRate = new BigDecimal("249.90");
        LocalDate normalDate = LocalDate.of(2027, 3, 10);

        BigDecimal price = PriceCalculator.calculateNightPrice(baseRate, normalDate);
        assertEquals(new BigDecimal("249.90"), price);
    }

    @Test
    public void testCalculateNightPriceHighSeason() {
        BigDecimal baseRate = new BigDecimal("249.90");
        LocalDate highSeasonDate = LocalDate.of(2027, 7, 10);

        BigDecimal price = PriceCalculator.calculateNightPrice(baseRate, highSeasonDate);
        assertEquals(new BigDecimal("299.88"), price);
    }

    @Test
    public void testCalculateDiscountNonRefundable() {
        BigDecimal subtotal = new BigDecimal("599.76");

        BigDecimal discount = PriceCalculator.calculateDiscount(subtotal, true);
        assertEquals(new BigDecimal("89.96"), discount);
    }

    @Test
    public void testCalculateDiscountStandard() {
        BigDecimal subtotal = new BigDecimal("599.76");

        BigDecimal discount = PriceCalculator.calculateDiscount(subtotal, false);
        assertEquals(BigDecimal.ZERO, discount);
    }
}
