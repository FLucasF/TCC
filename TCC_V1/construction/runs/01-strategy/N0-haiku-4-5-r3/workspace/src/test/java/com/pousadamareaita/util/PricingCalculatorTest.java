package com.pousadamareaita.util;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class PricingCalculatorTest {

    private void assertBigDecimalEquals(BigDecimal expected, BigDecimal actual, String message) {
        assertEquals(0, expected.compareTo(actual), message);
    }

    @Test
    public void testStandardPlan() {
        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);
        BigDecimal nightlyRate = new BigDecimal("249.90");

        PricingCalculator.PricingResult result = PricingCalculator.calculatePrice(
            101, checkIn, checkOut, "STANDARD", null, nightlyRate
        );

        assertEquals(2, result.nights);
        assertBigDecimalEquals(new BigDecimal("499.80"), result.subtotal, "Subtotal mismatch");
        assertBigDecimalEquals(new BigDecimal("0.00"), result.discount, "Discount mismatch");
        assertBigDecimalEquals(new BigDecimal("499.80"), result.total, "Total mismatch");
    }

    @Test
    public void testNonRefundablePlan() {
        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);
        BigDecimal nightlyRate = new BigDecimal("389.90");

        PricingCalculator.PricingResult result = PricingCalculator.calculatePrice(
            201, checkIn, checkOut, "NON_REFUNDABLE", null, nightlyRate
        );

        assertEquals(2, result.nights);
        assertBigDecimalEquals(new BigDecimal("779.80"), result.subtotal, "Subtotal mismatch");
        assertBigDecimalEquals(new BigDecimal("116.97"), result.discount, "Discount mismatch");
        assertBigDecimalEquals(new BigDecimal("662.83"), result.total, "Total mismatch");
    }

    @Test
    public void testCorporateTecnosulPlan() {
        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);
        BigDecimal nightlyRate = new BigDecimal("389.90");

        PricingCalculator.PricingResult result = PricingCalculator.calculatePrice(
            201, checkIn, checkOut, "CORPORATE", "TECNOSUL", nightlyRate
        );

        assertEquals(2, result.nights);
        assertBigDecimalEquals(new BigDecimal("779.80"), result.subtotal, "Subtotal mismatch");
        assertBigDecimalEquals(new BigDecimal("93.58"), result.discount, "Discount mismatch");
        assertBigDecimalEquals(new BigDecimal("686.22"), result.total, "Total mismatch");
    }

    @Test
    public void testWeekendPackage() {
        LocalDate checkIn = LocalDate.of(2027, 3, 12); // Friday
        LocalDate checkOut = LocalDate.of(2027, 3, 14); // Sunday
        BigDecimal nightlyRate = new BigDecimal("389.90");

        PricingCalculator.PricingResult result = PricingCalculator.calculatePrice(
            201, checkIn, checkOut, "WEEKEND_PACKAGE", null, nightlyRate
        );

        assertEquals(2, result.nights);
        assertBigDecimalEquals(new BigDecimal("820.00"), result.subtotal, "Subtotal mismatch");
        assertBigDecimalEquals(new BigDecimal("0.00"), result.discount, "Discount mismatch");
        assertBigDecimalEquals(new BigDecimal("820.00"), result.total, "Total mismatch");
    }

    @Test
    public void testWeekendPackageRefund() {
        BigDecimal amountPaid = new BigDecimal("820.00");
        BigDecimal refund = PricingCalculator.calculateRefund("WEEKEND_PACKAGE", amountPaid);

        assertBigDecimalEquals(new BigDecimal("410.00"), refund, "Refund mismatch");
    }

    @Test
    public void testLongStayPlan() {
        LocalDate checkIn = LocalDate.of(2027, 3, 1);
        LocalDate checkOut = LocalDate.of(2027, 3, 8);
        BigDecimal nightlyRate = new BigDecimal("249.90");

        PricingCalculator.PricingResult result = PricingCalculator.calculatePrice(
            101, checkIn, checkOut, "LONG_STAY", null, nightlyRate
        );

        assertEquals(7, result.nights);
        assertBigDecimalEquals(new BigDecimal("1749.30"), result.subtotal, "Subtotal mismatch");
        assertBigDecimalEquals(new BigDecimal("174.93"), result.discount, "Discount mismatch");
        assertBigDecimalEquals(new BigDecimal("1574.37"), result.total, "Total mismatch");
    }

    @Test
    public void testHighSeasonMultiplier() {
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);
        BigDecimal nightlyRate = new BigDecimal("249.90");

        PricingCalculator.PricingResult result = PricingCalculator.calculatePrice(
            101, checkIn, checkOut, "STANDARD", null, nightlyRate
        );

        assertEquals(2, result.nights);
        assertBigDecimalEquals(new BigDecimal("599.76"), result.subtotal, "Subtotal mismatch");
    }
}
