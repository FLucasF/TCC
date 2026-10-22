package com.pousada;

import com.pousada.utils.PriceCalculator;
import org.junit.Test;
import static org.junit.Assert.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public class PriceCalculatorTest {
    @Test
    public void testStandardRoomNormalSeason() {
        BigDecimal rate = new BigDecimal("249.90");
        LocalDate checkIn = LocalDate.of(2027, 8, 10);
        LocalDate checkOut = LocalDate.of(2027, 8, 12);

        PriceCalculator.PriceBreakdown breakdown =
            PriceCalculator.calculate(rate, checkIn, checkOut, "STANDARD");

        assertEquals(2, breakdown.subtotal.scale());
        assertEquals(new BigDecimal("499.80"), breakdown.subtotal);
        assertEquals(new BigDecimal("0.00"), breakdown.discount);
        assertEquals(new BigDecimal("499.80"), breakdown.total);
    }

    @Test
    public void testStandardRoomHighSeason() {
        BigDecimal rate = new BigDecimal("249.90");
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);

        PriceCalculator.PriceBreakdown breakdown =
            PriceCalculator.calculate(rate, checkIn, checkOut, "STANDARD");

        BigDecimal expectedNightPrice = new BigDecimal("299.88");
        BigDecimal expectedSubtotal = new BigDecimal("599.76");
        assertEquals(expectedSubtotal, breakdown.subtotal);
        assertEquals(new BigDecimal("0.00"), breakdown.discount);
        assertEquals(expectedSubtotal, breakdown.total);
    }

    @Test
    public void testNonRefundableDiscount() {
        BigDecimal rate = new BigDecimal("389.90");
        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        PriceCalculator.PriceBreakdown breakdown =
            PriceCalculator.calculate(rate, checkIn, checkOut, "NON_REFUNDABLE");

        BigDecimal expectedSubtotal = new BigDecimal("779.80");
        BigDecimal expectedDiscount = new BigDecimal("116.97");
        BigDecimal expectedTotal = new BigDecimal("662.83");

        assertEquals(expectedSubtotal, breakdown.subtotal);
        assertEquals(expectedDiscount, breakdown.discount);
        assertEquals(expectedTotal, breakdown.total);
    }
}
