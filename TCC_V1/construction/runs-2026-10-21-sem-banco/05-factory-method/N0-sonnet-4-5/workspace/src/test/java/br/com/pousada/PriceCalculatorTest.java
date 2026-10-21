package br.com.pousada;

import br.com.pousada.model.RatePlan;
import br.com.pousada.util.PriceCalculator;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class PriceCalculatorTest {

    @Test
    void testHighSeasonJuly() {
        assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2027, 7, 1)));
        assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2027, 7, 15)));
        assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2027, 7, 31)));
    }

    @Test
    void testHighSeasonDecemberToFebruary() {
        assertFalse(PriceCalculator.isHighSeason(LocalDate.of(2027, 12, 14)));
        assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2027, 12, 15)));
        assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2027, 12, 31)));
        assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2028, 1, 1)));
        assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2028, 2, 28)));
        assertFalse(PriceCalculator.isHighSeason(LocalDate.of(2028, 3, 1)));
    }

    @Test
    void testLowSeason() {
        assertFalse(PriceCalculator.isHighSeason(LocalDate.of(2027, 3, 10)));
        assertFalse(PriceCalculator.isHighSeason(LocalDate.of(2027, 6, 30)));
        assertFalse(PriceCalculator.isHighSeason(LocalDate.of(2027, 8, 1)));
    }

    @Test
    void testHighSeasonPrice() {
        BigDecimal baseRate = new BigDecimal("249.90");
        LocalDate highSeasonDate = LocalDate.of(2027, 7, 10);
        BigDecimal nightPrice = PriceCalculator.calculateNightPrice(baseRate, highSeasonDate);
        assertEquals(new BigDecimal("299.88"), nightPrice);
    }

    @Test
    void testSubtotalCalculation() {
        BigDecimal baseRate = new BigDecimal("249.90");
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);
        BigDecimal subtotal = PriceCalculator.calculateSubtotal(baseRate, checkIn, checkOut);
        assertEquals(new BigDecimal("599.76"), subtotal);
    }

    @Test
    void testNonRefundableDiscount() {
        BigDecimal subtotal = new BigDecimal("779.80");
        BigDecimal discount = PriceCalculator.calculateDiscount(subtotal, RatePlan.NON_REFUNDABLE);
        assertEquals(new BigDecimal("116.97"), discount);
    }

    @Test
    void testStandardPlanNoDiscount() {
        BigDecimal subtotal = new BigDecimal("779.80");
        BigDecimal discount = PriceCalculator.calculateDiscount(subtotal, RatePlan.STANDARD);
        assertEquals(BigDecimal.ZERO, discount);
    }

    @Test
    void testIssCalculation() {
        BigDecimal total = new BigDecimal("779.80");
        BigDecimal iss = PriceCalculator.calculateIss(total);
        assertEquals(new BigDecimal("38.99"), iss);
    }
}
