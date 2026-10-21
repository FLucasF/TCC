package com.pousadamarealta.util;

import com.pousadamarealta.domain.RatePlan;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

public class PriceCalculatorTest {
    @Test
    public void testHighSeasonStandardRate() {
        BigDecimal nightlyRate = new BigDecimal("249.90");
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);

        PriceCalculator.PriceBreakdown breakdown = PriceCalculator.calculate(nightlyRate, checkIn, checkOut, RatePlan.STANDARD);

        assertEquals(2, breakdown.nights);
        assertEquals(new BigDecimal("599.76"), breakdown.subtotal);
        assertEquals(BigDecimal.ZERO.setScale(2, java.math.RoundingMode.HALF_EVEN), breakdown.discount);
        assertEquals(new BigDecimal("599.76"), breakdown.total);
    }

    @Test
    public void testNonRefundableDiscount() {
        BigDecimal nightlyRate = new BigDecimal("389.90");
        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        PriceCalculator.PriceBreakdown breakdown = PriceCalculator.calculate(nightlyRate, checkIn, checkOut, RatePlan.NON_REFUNDABLE);

        assertEquals(2, breakdown.nights);
        assertEquals(new BigDecimal("779.80"), breakdown.subtotal);
        assertEquals(new BigDecimal("116.97"), breakdown.discount);
        assertEquals(new BigDecimal("662.83"), breakdown.total);
    }

    @Test
    public void testMixedHighAndLowSeason() {
        BigDecimal nightlyRate = new BigDecimal("249.90");
        LocalDate checkIn = LocalDate.of(2026, 12, 14);
        LocalDate checkOut = LocalDate.of(2026, 12, 17);

        PriceCalculator.PriceBreakdown breakdown = PriceCalculator.calculate(nightlyRate, checkIn, checkOut, RatePlan.STANDARD);

        assertEquals(3, breakdown.nights);
        // 1 low season (249.90) + 2 high season (249.90 * 1.2 = 299.88 each)
        assertEquals(new BigDecimal("849.66"), breakdown.subtotal);
        assertEquals(BigDecimal.ZERO.setScale(2, java.math.RoundingMode.HALF_EVEN), breakdown.discount);
        assertEquals(new BigDecimal("849.66"), breakdown.total);
    }
}
