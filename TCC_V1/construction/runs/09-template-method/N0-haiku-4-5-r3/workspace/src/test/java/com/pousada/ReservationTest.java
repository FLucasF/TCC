package com.pousada;

import com.pousada.utils.PricingUtils;
import com.pousada.utils.FormatUtils;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.Assert.*;

public class ReservationTest {
    @Test
    public void testPeakSeasonDetection() {
        // July is always peak season
        assertTrue(PricingUtils.isPeakSeason(LocalDate.parse("2027-07-10")));

        // December 15+ is peak season
        assertTrue(PricingUtils.isPeakSeason(LocalDate.parse("2027-12-15")));
        assertFalse(PricingUtils.isPeakSeason(LocalDate.parse("2027-12-14")));

        // January and February are peak season
        assertTrue(PricingUtils.isPeakSeason(LocalDate.parse("2027-01-01")));
        assertTrue(PricingUtils.isPeakSeason(LocalDate.parse("2027-02-28")));

        // April is not peak season
        assertFalse(PricingUtils.isPeakSeason(LocalDate.parse("2027-04-01")));
    }

    @Test
    public void testNightlyRateCalculation() {
        BigDecimal baseRate = new BigDecimal("249.90");

        // Peak season: 249.90 * 1.20 = 299.88
        BigDecimal peakRate = PricingUtils.calculateNightlyRate(baseRate, LocalDate.parse("2027-07-10"));
        assertEquals(new BigDecimal("299.88"), peakRate);

        // Off-season: 249.90
        BigDecimal offSeasonRate = PricingUtils.calculateNightlyRate(baseRate, LocalDate.parse("2027-04-01"));
        assertEquals(new BigDecimal("249.90"), offSeasonRate);
    }

    @Test
    public void testSubtotalCalculation() {
        BigDecimal baseRate = new BigDecimal("249.90");
        LocalDate checkIn = LocalDate.parse("2027-07-10");
        LocalDate checkOut = LocalDate.parse("2027-07-12");

        // 2 nights in July: 299.88 * 2 = 599.76
        BigDecimal subtotal = PricingUtils.calculateSubtotal(baseRate, checkIn, checkOut);
        assertEquals(new BigDecimal("599.76"), subtotal);
    }

    @Test
    public void testDiscountCalculation() {
        BigDecimal subtotal = new BigDecimal("599.76");

        // Standard plan: no discount
        BigDecimal standardDiscount = PricingUtils.calculateDiscount("STANDARD", subtotal);
        assertEquals(BigDecimal.ZERO, standardDiscount);

        // Non-refundable: 15% discount = 599.76 * 0.15 = 89.964 -> 89.96
        BigDecimal nonRefundableDiscount = PricingUtils.calculateDiscount("NON_REFUNDABLE", subtotal);
        assertEquals(new BigDecimal("89.96"), nonRefundableDiscount);
    }

    @Test
    public void testTotalCalculation() {
        BigDecimal subtotal = new BigDecimal("599.76");
        BigDecimal discount = new BigDecimal("89.96");

        BigDecimal total = PricingUtils.calculateTotal(subtotal, discount);
        assertEquals(new BigDecimal("509.80"), total);
    }

    @Test
    public void testNightsCalculation() {
        LocalDate checkIn = LocalDate.parse("2027-07-10");
        LocalDate checkOut = LocalDate.parse("2027-07-12");

        int nights = PricingUtils.calculateNights(checkIn, checkOut);
        assertEquals(2, nights);
    }

    @Test
    public void testFormatting() {
        LocalDate date = LocalDate.parse("2027-07-10");
        assertEquals("10/07/2027", FormatUtils.formatDate(date));
        assertEquals("2027-07-10", FormatUtils.formatDateIso(date));

        BigDecimal value = new BigDecimal("599.76");
        assertEquals("R$ 599,76", FormatUtils.formatCurrency(value));
        assertEquals("599.76", FormatUtils.formatCurrencyCSV(value));

        String cpf = "11122233344";
        assertEquals("111.222.333-44", FormatUtils.formatCPF(cpf));
    }
}
